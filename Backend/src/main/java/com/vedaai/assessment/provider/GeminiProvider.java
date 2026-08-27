package com.vedaai.assessment.provider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vedaai.assessment.config.AppProperties;
import com.vedaai.assessment.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Gemini Provider — handles multimodal extraction, segmentation, and batch grading.
 * Features rate-limit backoff retry (429 handling) and batch grading.
 */
@Component
public class GeminiProvider implements LlmProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiProvider.class);
    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";

    // Working models in precedence order
    private static final List<String> FALLBACK_MODELS = List.of(
            "gemini-3.6-flash",
            "gemini-3.7-flash"
    );

    // Max pages to send in a single multimodal request (at 150 DPI)
    private static final int BATCH_SIZE = 8;

    private final AppProperties props;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GeminiProvider(AppProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper.copy()
                .configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true)
                .configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    private List<String> getCandidateModels() {
        List<String> models = new ArrayList<>();
        String primary = props.getGeminiModel();
        if (primary != null && !primary.isBlank()) {
            models.add(primary.trim());
        }
        for (String fb : FALLBACK_MODELS) {
            if (!models.contains(fb)) {
                models.add(fb);
            }
        }
        return models;
    }

    private String getEndpointUrl(String modelName) {
        return GEMINI_BASE_URL + modelName + ":generateContent?key=" + props.getGeminiApiKey();
    }

    // ========================================================================================
    //  MULTIMODAL EXTRACTION — Send page images directly to Gemini
    // ========================================================================================

    public List<ExtractedQuestion> extractQuestionsFromImages(List<BufferedImage> pageImages) throws Exception {
        log.info("Extracting questions from {} page images via Gemini multimodal", pageImages.size());

        List<ExtractedQuestion> allQuestions = new ArrayList<>();
        List<List<BufferedImage>> batches = splitIntoBatches(pageImages, BATCH_SIZE);

        for (int batchIdx = 0; batchIdx < batches.size(); batchIdx++) {
            List<BufferedImage> batch = batches.get(batchIdx);
            int startPage = batchIdx * BATCH_SIZE + 1;
            int endPage = startPage + batch.size() - 1;
            log.info("Processing question paper batch {}/{} (pages {}-{})", batchIdx + 1, batches.size(), startPage, endPage);

            String prompt = String.format("""
                    You are analyzing a question paper. The images below show pages %d to %d of the paper.
                    
                    Extract ALL individual questions visible on these pages in their printed order.
                    If a question has sub-parts (e.g., 11(a), 11(b)), treat each sub-part as a SEPARATE question.
                    
                    For each question, provide:
                    - questionNumber: the full label (e.g., "1", "2", "11(a)", "11(b)")
                    - parentNumber: the main number (e.g., "1", "2", "11")
                    - subPart: the sub-part letter if any (e.g., "a", "b"), or null
                    - displayLabel: human-readable label (e.g., "1", "2", "11 a.", "11 b.")
                    - text: the full question text exactly as written
                    - maxScore: maximum marks if visible next to the question, otherwise 5
                    
                    IMPORTANT: Return ONLY a JSON array, no other text. Example:
                    [
                      {
                        "questionNumber": "1",
                        "parentNumber": "1",
                        "subPart": null,
                        "displayLabel": "1",
                        "text": "Which blood vessel carries blood away from the heart?",
                        "maxScore": 2
                      }
                    ]
                    """, startPage, endPage);

            String response = callGeminiMultimodalWithFallbacks(prompt, batch);
            allQuestions.addAll(parseQuestionResponse(response));
        }

        // Deduplicate questions with the same questionNumber
        Map<String, ExtractedQuestion> deduped = new LinkedHashMap<>();
        for (ExtractedQuestion q : allQuestions) {
            deduped.putIfAbsent(q.getQuestionNumber(), q);
        }
        List<ExtractedQuestion> result = new ArrayList<>(deduped.values());
        for (int i = 0; i < result.size(); i++) {
            result.get(i).setId("q" + (i + 1));
            result.get(i).setDisplayOrder(i + 1);
        }

        log.info("Total questions extracted: {}", result.size());
        return result;
    }

    public List<ExtractedAnswer> extractAnswersFromImages(List<BufferedImage> pageImages) throws Exception {
        log.info("Extracting answers from {} page images via Gemini multimodal", pageImages.size());

        List<ExtractedAnswer> allAnswers = new ArrayList<>();
        List<List<BufferedImage>> batches = splitIntoBatches(pageImages, BATCH_SIZE);

        for (int batchIdx = 0; batchIdx < batches.size(); batchIdx++) {
            List<BufferedImage> batch = batches.get(batchIdx);
            int startPage = batchIdx * BATCH_SIZE + 1;
            int endPage = startPage + batch.size() - 1;
            log.info("Processing answer sheet batch {}/{} (pages {}-{})", batchIdx + 1, batches.size(), startPage, endPage);

            String prompt = String.format("""
                    You are analyzing a student's handwritten answer sheet. The images below show pages %d to %d.
                    
                    Segment the content into individual answer blocks. Students may:
                    - Write answers out of order
                    - Label answers with numbers like "Q1", "1.", "Ans 1", "11(a)", etc.
                    - Write answers without any visible label
                    
                    For each answer block, provide:
                    - detectedLabel: the question label the student wrote (e.g., "Q1", "1", "11(a)"), or null if no label visible
                    - text: the full answer text (clean it up from handwriting if needed, produce readable text)
                    - regions: an array of bounding regions showing where this answer appears.
                      Each region has:
                        - page: the 1-based ABSOLUTE page number (first image is page %d)
                        - box: [ymin, xmin, ymax, xmax] coordinates on a 0-1000 scale relative to the page image
                    
                    IMPORTANT: Return ONLY a JSON array, no other text. Example:
                    [
                      {
                        "detectedLabel": "Q1",
                        "text": "The arteries carry blood away from the heart to various organs...",
                        "regions": [
                          {"page": %d, "box": [120, 50, 450, 950]}
                        ]
                      }
                    ]
                    """, startPage, endPage, startPage, startPage);

            String response = callGeminiMultimodalWithFallbacks(prompt, batch);
            allAnswers.addAll(parseAnswerWithRegionsResponse(response));
        }

        // Re-assign sequential IDs across batches
        for (int i = 0; i < allAnswers.size(); i++) {
            allAnswers.get(i).setId("a" + (i + 1));
        }

        log.info("Total answer blocks extracted: {}", allAnswers.size());
        return allAnswers;
    }

    // ========================================================================================
    //  TEXT-ONLY LLM METHODS
    // ========================================================================================

    @Override
    public List<ExtractedQuestion> segmentQuestions(List<WordEntry> words) throws Exception {
        String wordListText = formatWordList(words);

        String prompt = """
                You are analyzing OCR output from a question paper. Each word has an ID and text.
                
                Segment the text into individual questions in their original printed order.
                If a question has sub-parts (e.g., 11(a), 11(b)), treat each sub-part as a SEPARATE question.
                
                For each question, provide:
                - questionNumber: the full label (e.g., "1", "2", "11(a)", "11(b)")
                - parentNumber: the main number (e.g., "1", "2", "11")
                - subPart: the sub-part letter if any (e.g., "a", "b"), or null
                - displayLabel: human-readable label (e.g., "1", "2", "11 a.", "11 b.")
                - text: the full question text
                - startWordId: ID of the first word in this question
                - endWordId: ID of the last word in this question
                - maxScore: estimated maximum marks if visible, otherwise 5
                
                IMPORTANT: Return ONLY a JSON array, no other text.
                """ + wordListText;

        String response = callGeminiTextWithFallbacks(prompt);
        return parseQuestionResponse(response);
    }

    @Override
    public List<ExtractedAnswer> segmentAnswers(List<WordEntry> words) throws Exception {
        String wordListText = formatWordList(words);

        String prompt = """
                You are analyzing OCR output from a student's handwritten answer sheet.
                Segment the text into individual answer blocks.
                
                For each answer block, provide:
                - detectedLabel: the question label the student wrote (e.g., "Q1", "1", "11(a)"), or null if no label visible
                - text: the full answer text (clean it up from OCR if needed)
                - startWordId: ID of the first word in this answer
                - endWordId: ID of the last word in this answer
                
                IMPORTANT: Return ONLY a JSON array, no other text.
                """ + wordListText;

        String response = callGeminiTextWithFallbacks(prompt);
        return parseAnswerResponse(response);
    }

    @Override
    public Map<String, String> semanticMatch(List<ExtractedQuestion> questions,
                                              List<ExtractedAnswer> unmatchedAnswers) throws Exception {
        if (unmatchedAnswers.isEmpty()) return Map.of();

        StringBuilder prompt = new StringBuilder();
        prompt.append("""
                Match the following unmatched student answers to their most likely questions
                based on content similarity.
                
                Questions:
                """);

        for (ExtractedQuestion q : questions) {
            prompt.append(String.format("- ID: %s, Number: %s, Text: %s\n", q.getId(), q.getQuestionNumber(), q.getText()));
        }

        prompt.append("\nUnmatched Answers:\n");
        for (ExtractedAnswer a : unmatchedAnswers) {
            prompt.append(String.format("- ID: %s, Label: %s, Text: %s\n",
                    a.getId(), a.getDetectedLabel(), truncate(a.getText(), 200)));
        }

        prompt.append("""
                
                Return ONLY a JSON object mapping answer IDs to question IDs.
                Only include matches you are confident about (>60% confidence).
                Example: {"a1": "q1", "a3": "q5"}
                """);

        String response = callGeminiTextWithFallbacks(prompt.toString());
        return parseMatchResponse(response);
    }

    @Override
    public GradingResult gradeAnswer(String questionText, String answerText, int maxScore) throws Exception {
        String prompt = String.format("""
                Grade the following student answer against the question.
                
                Question: %s
                Student's Answer: %s
                Maximum Score: %d
                
                Return ONLY a JSON object:
                {
                  "score": <integer 0 to %d>,
                  "maxScore": %d,
                  "status": "<CORRECT|PARTIALLY_CORRECT|INCORRECT>",
                  "feedback": "<brief constructive feedback>",
                  "conceptsPresent": ["concept1", "concept2"],
                  "conceptsMissing": ["concept3"]
                }
                """, questionText, truncate(answerText, 1000), maxScore, maxScore, maxScore);

        String response = callGeminiTextWithFallbacks(prompt);
        return parseGradingResponse(response, maxScore);
    }

    @Override
    public Map<String, GradingResult> gradeBatch(List<GradingItem> items) throws Exception {
        if (items.isEmpty()) return Map.of();

        log.info("Grading {} items in batch", items.size());

        StringBuilder prompt = new StringBuilder();
        prompt.append("""
                You are grading a student's exam answers against the question paper.
                Grade each answer based on correctness, clarity, and conceptual coverage.
                
                Here are the question-answer pairs to grade:
                """);

        ArrayNode itemsNode = objectMapper.createArrayNode();
        for (GradingItem item : items) {
            ObjectNode obj = itemsNode.addObject();
            obj.put("questionId", item.questionId());
            obj.put("question", item.questionText());
            obj.put("answer", truncate(item.answerText(), 1200));
            obj.put("maxScore", item.maxScore());
        }
        prompt.append(objectMapper.writeValueAsString(itemsNode));

        prompt.append("""
                
                Return ONLY a JSON array with one object per question in the exact same order:
                [
                  {
                    "questionId": "q1",
                    "score": <integer 0 to maxScore>,
                    "maxScore": <maxScore>,
                    "status": "<CORRECT|PARTIALLY_CORRECT|INCORRECT>",
                    "feedback": "<brief constructive feedback>",
                    "conceptsPresent": ["concept1"],
                    "conceptsMissing": ["concept2"]
                  }
                ]
                """);

        String response = callGeminiTextWithFallbacks(prompt.toString());
        return parseBatchGradingResponse(response);
    }

    // ========================================================================================
    //  HTTP / FALLBACK CLIENT LOGIC WITH RATE LIMIT BACKOFF
    // ========================================================================================

    private String callGeminiMultimodalWithFallbacks(String prompt, List<BufferedImage> images) throws Exception {
        List<String> candidates = getCandidateModels();
        Exception lastException = null;

        for (String model : candidates) {
            try {
                log.info("Attempting multimodal extraction with model: {}", model);
                return executeMultimodalWithRetry(model, prompt, images);
            } catch (Exception e) {
                log.warn("Multimodal request failed with model {}: {}. Trying next fallback...", model, e.getMessage());
                lastException = e;
            }
        }
        throw new RuntimeException("All Gemini models failed for multimodal request. Last error: "
                + (lastException != null ? lastException.getMessage() : "unknown"));
    }

    private String callGeminiTextWithFallbacks(String prompt) throws Exception {
        List<String> candidates = getCandidateModels();
        Exception lastException = null;

        for (String model : candidates) {
            try {
                log.debug("Attempting text request with model: {}", model);
                return executeTextWithRetry(model, prompt);
            } catch (Exception e) {
                log.warn("Text request failed with model {}: {}. Trying next fallback...", model, e.getMessage());
                lastException = e;
            }
        }
        throw new RuntimeException("All Gemini models failed for text request. Last error: "
                + (lastException != null ? lastException.getMessage() : "unknown"));
    }

    private String executeMultimodalWithRetry(String model, String prompt, List<BufferedImage> images) throws Exception {
        int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return executeMultimodalRequest(model, prompt, images);
            } catch (RateLimitException rle) {
                if (attempt < maxAttempts) {
                    int sleepSec = Math.min(attempt * 6, 20);
                    log.warn("Rate limit (429) hit on model {}. Waiting {}s before retry (attempt {}/{})",
                            model, sleepSec, attempt, maxAttempts);
                    Thread.sleep(sleepSec * 1000L);
                } else {
                    throw rle;
                }
            }
        }
        throw new RuntimeException("Failed after " + maxAttempts + " attempts");
    }

    private String executeTextWithRetry(String model, String prompt) throws Exception {
        int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return executeTextRequest(model, prompt);
            } catch (RateLimitException rle) {
                if (attempt < maxAttempts) {
                    int sleepSec = Math.min(attempt * 6, 20);
                    log.warn("Rate limit (429) hit on model {}. Waiting {}s before retry (attempt {}/{})",
                            model, sleepSec, attempt, maxAttempts);
                    Thread.sleep(sleepSec * 1000L);
                } else {
                    throw rle;
                }
            }
        }
        throw new RuntimeException("Failed after " + maxAttempts + " attempts");
    }

    private String executeMultimodalRequest(String model, String prompt, List<BufferedImage> images) throws Exception {
        String url = getEndpointUrl(model);

        ObjectNode requestBody = objectMapper.createObjectNode();
        ArrayNode contents = requestBody.putArray("contents");
        ObjectNode content = contents.addObject();
        ArrayNode parts = content.putArray("parts");

        for (BufferedImage img : images) {
            String base64 = encodeImage(img);
            ObjectNode imagePart = parts.addObject();
            ObjectNode inlineData = imagePart.putObject("inlineData");
            inlineData.put("mimeType", "image/png");
            inlineData.put("data", base64);
        }

        parts.addObject().put("text", prompt);

        ObjectNode generationConfig = requestBody.putObject("generationConfig");
        generationConfig.put("temperature", 0.1);
        generationConfig.put("maxOutputTokens", 65536);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", props.getGeminiApiKey())
                .timeout(Duration.ofSeconds(180))
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 429) {
            log.warn("Gemini model {} returned 429 Rate Limit", model);
            throw new RateLimitException("Rate limit 429 on model " + model);
        }

        if (response.statusCode() != 200) {
            log.error("Gemini model {} returned status ({}): {}", model, response.statusCode(), response.body());
            throw new RuntimeException("Gemini model " + model + " returned status " + response.statusCode());
        }

        return extractCleanJson(response.body());
    }

    private String executeTextRequest(String model, String prompt) throws Exception {
        String url = getEndpointUrl(model);

        var requestBody = objectMapper.createObjectNode();
        var contents = requestBody.putArray("contents");
        var content = contents.addObject();
        var parts = content.putArray("parts");
        parts.addObject().put("text", prompt);

        var generationConfig = requestBody.putObject("generationConfig");
        generationConfig.put("temperature", 0.1);
        generationConfig.put("maxOutputTokens", 16384);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", props.getGeminiApiKey())
                .timeout(Duration.ofSeconds(90))
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 429) {
            log.warn("Gemini model {} returned 429 Rate Limit", model);
            throw new RateLimitException("Rate limit 429 on model " + model);
        }

        if (response.statusCode() != 200) {
            log.error("Gemini model {} returned status ({}): {}", model, response.statusCode(), response.body());
            throw new RuntimeException("Gemini model " + model + " returned status " + response.statusCode());
        }

        return extractCleanJson(response.body());
    }

    private String extractCleanJson(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode candidates = root.path("candidates");
        if (candidates.isEmpty()) {
            throw new RuntimeException("No candidates in Gemini response");
        }

        String text = candidates.get(0).path("content").path("parts").get(0).path("text").asText().strip();

        // Strip markdown backticks
        if (text.startsWith("```json")) {
            text = text.substring(7);
        } else if (text.startsWith("```")) {
            text = text.substring(3);
        }
        if (text.endsWith("```")) {
            text = text.substring(0, text.length() - 3);
        }
        text = text.strip();

        // Extract substring between first and last bracket to guarantee valid JSON boundary
        int firstBracket = text.indexOf('[');
        int lastBracket = text.lastIndexOf(']');
        int firstBrace = text.indexOf('{');
        int lastBrace = text.lastIndexOf('}');

        if (firstBracket != -1 && lastBracket != -1 && (firstBrace == -1 || firstBracket < firstBrace)) {
            text = text.substring(firstBracket, lastBracket + 1).strip();
        } else if (firstBrace != -1 && lastBrace != -1) {
            text = text.substring(firstBrace, lastBrace + 1).strip();
        }

        try {
            objectMapper.readTree(text);
        } catch (Exception e) {
            log.warn("Initial JSON read failed ({}), attempting backslash sanitization", e.getMessage());
            // Sanitize invalid backslashes (e.g. \alpha, \text, \approx that aren't valid JSON escapes)
            text = text.replaceAll("\\\\(?![\"\\\\/bfnrtu])", "\\\\\\\\");
            objectMapper.readTree(text);
        }
        return text;
    }

    private String encodeImage(BufferedImage image) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    private String formatWordList(List<WordEntry> words) {
        StringBuilder sb = new StringBuilder();
        int currentPage = -1;
        for (WordEntry w : words) {
            if (w.page() != currentPage) {
                currentPage = w.page();
                sb.append("\n--- Page ").append(currentPage).append(" ---\n");
            }
            sb.append(w.id()).append(": ").append(w.text()).append("\n");
        }
        return sb.toString();
    }

    private <T> List<List<T>> splitIntoBatches(List<T> list, int batchSize) {
        List<List<T>> batches = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            batches.add(list.subList(i, Math.min(i + batchSize, list.size())));
        }
        return batches;
    }

    // ========================================================================================
    //  RESPONSE PARSERS
    // ========================================================================================

    private List<ExtractedQuestion> parseQuestionResponse(String json) throws Exception {
        List<Map<String, Object>> items = objectMapper.readValue(json, new TypeReference<>() {});
        List<ExtractedQuestion> questions = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            Map<String, Object> item = items.get(i);
            String qNum = String.valueOf(item.get("questionNumber"));
            String parent = String.valueOf(item.getOrDefault("parentNumber", qNum));
            String subPart = item.get("subPart") != null ? String.valueOf(item.get("subPart")) : null;
            if ("null".equals(subPart)) subPart = null;
            String displayLabel = String.valueOf(item.getOrDefault("displayLabel", qNum));
            String text = String.valueOf(item.getOrDefault("text", ""));
            int maxScore = item.get("maxScore") != null ? ((Number) item.get("maxScore")).intValue() : 5;

            questions.add(ExtractedQuestion.builder()
                    .id("q" + (i + 1))
                    .questionNumber(qNum)
                    .parentNumber(parent)
                    .subPart(subPart)
                    .displayLabel(displayLabel)
                    .text(text)
                    .displayOrder(i + 1)
                    .wordIds(List.of())
                    .maxScore(maxScore)
                    .build());
        }

        return questions;
    }

    private List<ExtractedAnswer> parseAnswerWithRegionsResponse(String json) throws Exception {
        List<Map<String, Object>> items = objectMapper.readValue(json, new TypeReference<>() {});
        List<ExtractedAnswer> answers = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            Map<String, Object> item = items.get(i);
            String label = item.get("detectedLabel") != null ? String.valueOf(item.get("detectedLabel")) : null;
            if ("null".equals(label)) label = null;
            String text = String.valueOf(item.getOrDefault("text", ""));

            List<AnswerRegion> regions = new ArrayList<>();
            Object regionsObj = item.get("regions");
            if (regionsObj instanceof List<?> regionsList) {
                for (Object regionObj : regionsList) {
                    if (regionObj instanceof Map<?, ?> regionMap) {
                        int page = regionMap.get("page") != null ? ((Number) regionMap.get("page")).intValue() : 1;
                        Object boxObj = regionMap.get("box");
                        if (boxObj instanceof List<?> box && box.size() >= 4) {
                            double ymin = ((Number) box.get(0)).doubleValue() / 1000.0;
                            double xmin = ((Number) box.get(1)).doubleValue() / 1000.0;
                            double ymax = ((Number) box.get(2)).doubleValue() / 1000.0;
                            double xmax = ((Number) box.get(3)).doubleValue() / 1000.0;

                            regions.add(AnswerRegion.builder()
                                    .page(page)
                                    .x(xmin)
                                    .y(ymin)
                                    .width(xmax - xmin)
                                    .height(ymax - ymin)
                                    .build());
                        }
                    }
                }
            }

            answers.add(ExtractedAnswer.builder()
                    .id("a" + (i + 1))
                    .detectedLabel(label)
                    .text(text)
                    .wordIds(List.of())
                    .regions(regions)
                    .confidence(0.0)
                    .build());
        }

        log.info("Parsed {} answer blocks with bounding regions from Gemini", answers.size());
        return answers;
    }

    private List<ExtractedAnswer> parseAnswerResponse(String json) throws Exception {
        List<Map<String, Object>> items = objectMapper.readValue(json, new TypeReference<>() {});
        List<ExtractedAnswer> answers = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            Map<String, Object> item = items.get(i);
            String label = item.get("detectedLabel") != null ? String.valueOf(item.get("detectedLabel")) : null;
            if ("null".equals(label)) label = null;
            String text = String.valueOf(item.getOrDefault("text", ""));

            answers.add(ExtractedAnswer.builder()
                    .id("a" + (i + 1))
                    .detectedLabel(label)
                    .text(text)
                    .wordIds(List.of())
                    .confidence(0.0)
                    .build());
        }

        return answers;
    }

    private Map<String, String> parseMatchResponse(String json) throws Exception {
        return objectMapper.readValue(json, new TypeReference<>() {});
    }

    private GradingResult parseGradingResponse(String json, int maxScore) throws Exception {
        Map<String, Object> item = objectMapper.readValue(json, new TypeReference<>() {});

        int score = Math.min(((Number) item.getOrDefault("score", 0)).intValue(), maxScore);
        String statusStr = String.valueOf(item.getOrDefault("status", "REVIEW"));
        GradingStatus status;
        try {
            status = GradingStatus.valueOf(statusStr);
        } catch (IllegalArgumentException e) {
            status = GradingStatus.REVIEW;
        }

        List<String> conceptsPresent = item.get("conceptsPresent") instanceof List ?
                ((List<?>) item.get("conceptsPresent")).stream().map(String::valueOf).collect(Collectors.toList()) :
                List.of();
        List<String> conceptsMissing = item.get("conceptsMissing") instanceof List ?
                ((List<?>) item.get("conceptsMissing")).stream().map(String::valueOf).collect(Collectors.toList()) :
                List.of();

        return GradingResult.builder()
                .score(score)
                .maxScore(maxScore)
                .status(status)
                .feedback(String.valueOf(item.getOrDefault("feedback", "")))
                .conceptsPresent(conceptsPresent)
                .conceptsMissing(conceptsMissing)
                .build();
    }

    private Map<String, GradingResult> parseBatchGradingResponse(String json) throws Exception {
        List<Map<String, Object>> items = objectMapper.readValue(json, new TypeReference<>() {});
        Map<String, GradingResult> results = new LinkedHashMap<>();

        for (Map<String, Object> item : items) {
            String qId = String.valueOf(item.get("questionId"));
            int maxScore = item.get("maxScore") != null ? ((Number) item.get("maxScore")).intValue() : 5;
            int score = Math.min(((Number) item.getOrDefault("score", 0)).intValue(), maxScore);
            String statusStr = String.valueOf(item.getOrDefault("status", "REVIEW"));
            GradingStatus status;
            try {
                status = GradingStatus.valueOf(statusStr);
            } catch (IllegalArgumentException e) {
                status = GradingStatus.REVIEW;
            }

            List<String> conceptsPresent = item.get("conceptsPresent") instanceof List ?
                    ((List<?>) item.get("conceptsPresent")).stream().map(String::valueOf).collect(Collectors.toList()) :
                    List.of();
            List<String> conceptsMissing = item.get("conceptsMissing") instanceof List ?
                    ((List<?>) item.get("conceptsMissing")).stream().map(String::valueOf).collect(Collectors.toList()) :
                    List.of();

            results.put(qId, GradingResult.builder()
                    .score(score)
                    .maxScore(maxScore)
                    .status(status)
                    .feedback(String.valueOf(item.getOrDefault("feedback", "")))
                    .conceptsPresent(conceptsPresent)
                    .conceptsMissing(conceptsMissing)
                    .build());
        }

        return results;
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }

    private static class RateLimitException extends RuntimeException {
        public RateLimitException(String message) {
            super(message);
        }
    }
}
