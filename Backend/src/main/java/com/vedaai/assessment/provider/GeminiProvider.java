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
 * Gemini Provider — handles multimodal extraction, raw document extraction, segmentation, and batch grading.
 * Features rate-limit backoff retry (429 handling) and batch grading.
 */
@Component
public class GeminiProvider implements LlmProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiProvider.class);
    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";

    // Active working models in precedence order
    private static final List<String> FALLBACK_MODELS = List.of(
            "gemini-3.6-flash",
            "gemini-3.7-flash"
    );

    // Max pages to send in a single multimodal request
    private static final int BATCH_SIZE = 4;

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
    //  DIRECT RAW DOCUMENT EXTRACTION (0ms conversion for Question Paper PDF / Images)
    // ========================================================================================

    public List<ExtractedQuestion> extractQuestionsFromDocument(byte[] docBytes, String contentType) throws Exception {
        String mimeType = (contentType != null && !contentType.isBlank()) ? contentType : "application/pdf";
        log.info("Extracting questions directly from raw {} document ({} bytes) via Gemini", mimeType, docBytes.length);

        String prompt = """
                You are analyzing a question paper document.
                
                Extract ALL individual questions visible in this paper in their printed order.
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
                """;

        String response = callGeminiRawDocumentWithFallbacks(prompt, docBytes, mimeType);
        List<ExtractedQuestion> allQuestions = parseQuestionResponse(response);

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

        log.info("Total questions extracted directly: {}", result.size());
        return result;
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
                You are helping map student answers to exam questions.
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

        log.info("Grading {} items in chunks of 10", items.size());
        Map<String, GradingResult> allResults = new LinkedHashMap<>();

        List<List<GradingItem>> batches = splitIntoBatches(items, 10);
        for (int b = 0; b < batches.size(); b++) {
            List<GradingItem> batch = batches.get(b);
            log.info("Processing grading sub-batch {}/{} ({} questions)", b + 1, batches.size(), batch.size());

            StringBuilder prompt = new StringBuilder();
            prompt.append("""
                    You are grading a student's exam answers against the question paper.
                    Grade each answer based on correctness, clarity, and conceptual coverage.
                    
                    Here are the question-answer pairs to grade:
                    """);

            ArrayNode itemsNode = objectMapper.createArrayNode();
            for (GradingItem item : batch) {
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

            try {
                String response = callGeminiTextWithFallbacks(prompt.toString());
                Map<String, GradingResult> batchResults = parseBatchGradingResponse(response);
                allResults.putAll(batchResults);
                log.info("Grading sub-batch {}/{} succeeded with {} evaluations", b + 1, batches.size(), batchResults.size());
            } catch (Exception e) {
                log.warn("Grading sub-batch {}/{} failed: {}", b + 1, batches.size(), e.getMessage());
            }
        }

        return allResults;
    }

    // ========================================================================================
    //  HTTP / FALLBACK CLIENT LOGIC WITH RATE LIMIT BACKOFF
    // ========================================================================================

    private String callGeminiRawDocumentWithFallbacks(String prompt, byte[] docBytes, String mimeType) throws Exception {
        List<String> candidates = getCandidateModels();
        Exception lastException = null;

        for (String model : candidates) {
            try {
                log.info("Attempting direct document extraction with model: {}", model);
                return executeRawDocumentWithRetry(model, prompt, docBytes, mimeType);
            } catch (Exception e) {
                log.warn("Direct document request failed with model {}: {}. Trying next fallback...", model, e.getMessage());
                lastException = e;
            }
        }
        throw new RuntimeException("All Gemini models failed for direct document request. Last error: "
                + (lastException != null ? lastException.getMessage() : "unknown"));
    }

    private String executeRawDocumentWithRetry(String model, String prompt, byte[] docBytes, String mimeType) throws Exception {
        int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return executeRawDocumentRequest(model, prompt, docBytes, mimeType);
            } catch (RateLimitException rle) {
                if (attempt < maxAttempts) {
                    int sleepSec = Math.min(attempt * 4, 15);
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

    private String executeRawDocumentRequest(String model, String prompt, byte[] docBytes, String mimeType) throws Exception {
        String url = getEndpointUrl(model);

        ObjectNode requestBody = objectMapper.createObjectNode();
        ArrayNode contents = requestBody.putArray("contents");
        ObjectNode content = contents.addObject();
        ArrayNode parts = content.putArray("parts");

        String base64 = Base64.getEncoder().encodeToString(docBytes);
        ObjectNode docPart = parts.addObject();
        ObjectNode inlineData = docPart.putObject("inlineData");
        inlineData.put("mimeType", mimeType);
        inlineData.put("data", base64);

        parts.addObject().put("text", prompt);

        ObjectNode generationConfig = requestBody.putObject("generationConfig");
        generationConfig.put("temperature", 0.1);

        String requestJson = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 429) {
            throw new RateLimitException("Gemini 429 rate limit");
        }
        if (response.statusCode() != 200) {
            throw new RuntimeException("Gemini API error (status " + response.statusCode() + "): " + response.body());
        }

        return extractTextFromResponse(response.body());
    }

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
        int maxAttempts = 5;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return executeMultimodalRequest(model, prompt, images);
            } catch (RateLimitException rle) {
                if (attempt < maxAttempts) {
                    int sleepSec = Math.min(attempt * 2, 8);
                    log.warn("Temporary rate limit or 503 on model {}. Retrying in {}s (attempt {}/{})",
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
        int maxAttempts = 5;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return executeTextRequest(model, prompt);
            } catch (RateLimitException rle) {
                if (attempt < maxAttempts) {
                    int sleepSec = Math.min(attempt * 2, 8);
                    log.warn("Temporary rate limit or 503 on model {}. Retrying in {}s (attempt {}/{})",
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
            inlineData.put("mimeType", "image/jpeg");
            inlineData.put("data", base64);
        }

        parts.addObject().put("text", prompt);

        ObjectNode generationConfig = requestBody.putObject("generationConfig");
        generationConfig.put("temperature", 0.1);

        String requestJson = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(120))
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 429 || response.statusCode() == 503) {
            throw new RateLimitException("Gemini temporary issue (status " + response.statusCode() + "): " + response.body());
        }
        if (response.statusCode() != 200) {
            throw new RuntimeException("Gemini API error (status " + response.statusCode() + "): " + response.body());
        }

        return extractTextFromResponse(response.body());
    }

    private String executeTextRequest(String model, String prompt) throws Exception {
        String url = getEndpointUrl(model);

        ObjectNode requestBody = objectMapper.createObjectNode();
        ArrayNode contents = requestBody.putArray("contents");
        ObjectNode content = contents.addObject();
        ArrayNode parts = content.putArray("parts");
        parts.addObject().put("text", prompt);

        ObjectNode generationConfig = requestBody.putObject("generationConfig");
        generationConfig.put("temperature", 0.1);

        String requestJson = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(120))
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 429 || response.statusCode() == 503) {
            throw new RateLimitException("Gemini temporary issue (status " + response.statusCode() + "): " + response.body());
        }
        if (response.statusCode() != 200) {
            throw new RuntimeException("Gemini API error (status " + response.statusCode() + "): " + response.body());
        }

        return extractTextFromResponse(response.body());
    }

    // ========================================================================================
    //  RESPONSE PARSERS
    // ========================================================================================

    private String extractTextFromResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode candidates = root.get("candidates");
        if (candidates == null || !candidates.isArray() || candidates.isEmpty()) {
            throw new RuntimeException("No candidates in Gemini response: " + responseBody);
        }
        JsonNode firstCandidate = candidates.get(0);
        JsonNode content = firstCandidate.get("content");
        if (content == null) {
            throw new RuntimeException("No content in first candidate: " + responseBody);
        }
        JsonNode parts = content.get("parts");
        if (parts == null || !parts.isArray() || parts.isEmpty()) {
            throw new RuntimeException("No parts in candidate content: " + responseBody);
        }
        String text = parts.get(0).get("text").asText();
        return cleanJsonText(text);
    }

    private String cleanJsonText(String text) {
        if (text == null) return "[]";
        text = text.trim();
        if (text.startsWith("```json")) {
            text = text.substring(7);
        } else if (text.startsWith("```")) {
            text = text.substring(3);
        }
        if (text.endsWith("```")) {
            text = text.substring(0, text.length() - 3);
        }
        text = text.trim();

        int firstBracket = text.indexOf('[');
        int firstBrace = text.indexOf('{');
        int start = -1;
        if (firstBracket != -1 && firstBrace != -1) {
            start = Math.min(firstBracket, firstBrace);
        } else if (firstBracket != -1) {
            start = firstBracket;
        } else if (firstBrace != -1) {
            start = firstBrace;
        }

        int lastBracket = text.lastIndexOf(']');
        int lastBrace = text.lastIndexOf('}');
        int end = Math.max(lastBracket, lastBrace);

        if (start != -1 && end != -1 && end > start) {
            text = text.substring(start, end + 1);
        }
        return text.trim();
    }

    private List<ExtractedQuestion> parseQuestionResponse(String json) throws Exception {
        List<Map<String, Object>> items = objectMapper.readValue(json, new TypeReference<>() {});
        List<ExtractedQuestion> questions = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            Map<String, Object> item = items.get(i);
            String qNum = String.valueOf(item.getOrDefault("questionNumber", String.valueOf(i + 1)));
            String pNum = String.valueOf(item.getOrDefault("parentNumber", qNum));
            String subPart = item.get("subPart") != null ? String.valueOf(item.get("subPart")) : null;
            String displayLabel = String.valueOf(item.getOrDefault("displayLabel", qNum));
            String text = String.valueOf(item.getOrDefault("text", ""));
            int maxScore = item.get("maxScore") != null ? ((Number) item.get("maxScore")).intValue() : 5;

            questions.add(ExtractedQuestion.builder()
                    .id("q" + (i + 1))
                    .questionNumber(qNum)
                    .parentNumber(pNum)
                    .subPart(subPart)
                    .displayLabel(displayLabel)
                    .text(text)
                    .maxScore(maxScore)
                    .displayOrder(i + 1)
                    .build());
        }

        return questions;
    }

    private List<ExtractedAnswer> parseAnswerWithRegionsResponse(String json) throws Exception {
        List<Map<String, Object>> items = objectMapper.readValue(json, new TypeReference<>() {});
        List<ExtractedAnswer> answers = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            Map<String, Object> item = items.get(i);
            String detectedLabel = item.get("detectedLabel") != null ? String.valueOf(item.get("detectedLabel")) : null;
            String text = String.valueOf(item.getOrDefault("text", ""));

            List<AnswerRegion> regions = new ArrayList<>();
            Object regionsObj = item.get("regions");
            if (regionsObj instanceof List<?> regList) {
                for (Object r : regList) {
                    if (r instanceof Map<?, ?> regMap) {
                        int page = regMap.get("page") != null ? ((Number) regMap.get("page")).intValue() : 1;
                        Object boxObj = regMap.get("box");
                        if (boxObj instanceof List<?> box && box.size() >= 4) {
                            // box is [ymin, xmin, ymax, xmax] on 0-1000 scale
                            double ymin = ((Number) box.get(0)).doubleValue() / 1000.0;
                            double xmin = ((Number) box.get(1)).doubleValue() / 1000.0;
                            double ymax = ((Number) box.get(2)).doubleValue() / 1000.0;
                            double xmax = ((Number) box.get(3)).doubleValue() / 1000.0;

                            regions.add(AnswerRegion.builder()
                                    .page(page)
                                    .x(xmin)
                                    .y(ymin)
                                    .width(Math.max(0.01, xmax - xmin))
                                    .height(Math.max(0.01, ymax - ymin))
                                    .build());
                        }
                    }
                }
            }

            // Fallback region if none returned
            if (regions.isEmpty()) {
                regions.add(AnswerRegion.builder()
                        .page(1)
                        .x(0.05)
                        .y(0.1 + (i * 0.15) % 0.8)
                        .width(0.9)
                        .height(0.12)
                        .build());
            }

            answers.add(ExtractedAnswer.builder()
                    .id("a" + (i + 1))
                    .detectedLabel(detectedLabel)
                    .text(text)
                    .regions(regions)
                    .build());
        }

        return answers;
    }

    private List<ExtractedAnswer> parseAnswerResponse(String json) throws Exception {
        List<Map<String, Object>> items = objectMapper.readValue(json, new TypeReference<>() {});
        List<ExtractedAnswer> answers = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            Map<String, Object> item = items.get(i);
            String detectedLabel = item.get("detectedLabel") != null ? String.valueOf(item.get("detectedLabel")) : null;
            String text = String.valueOf(item.getOrDefault("text", ""));

            answers.add(ExtractedAnswer.builder()
                    .id("a" + (i + 1))
                    .detectedLabel(detectedLabel)
                    .text(text)
                    .regions(List.of())
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
            status = GradingStatus.valueOf(statusStr.toUpperCase());
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
        JsonNode root = objectMapper.readTree(json);
        ArrayNode arrayNode = null;
        if (root.isArray()) {
            arrayNode = (ArrayNode) root;
        } else if (root.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
            while (fields.hasNext()) {
                JsonNode child = fields.next().getValue();
                if (child.isArray()) {
                    arrayNode = (ArrayNode) child;
                    break;
                }
            }
        }

        if (arrayNode == null) {
            log.warn("Could not extract array from grading JSON: {}", json);
            return Map.of();
        }

        Map<String, GradingResult> results = new LinkedHashMap<>();

        for (JsonNode item : arrayNode) {
            String qId = item.has("questionId") ? item.get("questionId").asText() : "";
            int maxScore = item.has("maxScore") ? item.get("maxScore").asInt(5) : 5;
            int score = item.has("score") ? Math.min(item.get("score").asInt(0), maxScore) : 0;
            String statusStr = item.has("status") ? item.get("status").asText("REVIEW") : "REVIEW";
            GradingStatus status;
            try {
                status = GradingStatus.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                status = GradingStatus.REVIEW;
            }

            List<String> conceptsPresent = new ArrayList<>();
            if (item.has("conceptsPresent") && item.get("conceptsPresent").isArray()) {
                for (JsonNode c : item.get("conceptsPresent")) {
                    conceptsPresent.add(c.asText());
                }
            }

            List<String> conceptsMissing = new ArrayList<>();
            if (item.has("conceptsMissing") && item.get("conceptsMissing").isArray()) {
                for (JsonNode c : item.get("conceptsMissing")) {
                    conceptsMissing.add(c.asText());
                }
            }

            String feedback = item.has("feedback") ? item.get("feedback").asText() : "";

            GradingResult gr = GradingResult.builder()
                    .score(score)
                    .maxScore(maxScore)
                    .status(status)
                    .feedback(feedback)
                    .conceptsPresent(conceptsPresent)
                    .conceptsMissing(conceptsMissing)
                    .build();

            if (!qId.isBlank()) {
                results.put(qId, gr);
                results.put(qId.toLowerCase(), gr);
                results.put(qId.replaceFirst("^[Qq]", ""), gr);
            }
        }

        return results;
    }

    // ========================================================================================
    //  HELPERS
    // ========================================================================================

    private String encodeImage(BufferedImage image) throws Exception {
        BufferedImage rgbImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = rgbImage.createGraphics();
        g.drawImage(image, 0, 0, java.awt.Color.WHITE, null);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(rgbImage, "JPEG", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    private String formatWordList(List<WordEntry> words) {
        StringBuilder sb = new StringBuilder("Words (ID: text):\n");
        for (WordEntry w : words) {
            sb.append(String.format("%s: %s\n", w.id(), w.text()));
        }
        return sb.toString();
    }

    private <T> List<List<T>> splitIntoBatches(List<T> list, int size) {
        List<List<T>> batches = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            batches.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return batches;
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
