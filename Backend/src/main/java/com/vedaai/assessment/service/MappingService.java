package com.vedaai.assessment.service;

import com.vedaai.assessment.model.*;
import com.vedaai.assessment.provider.LlmProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Four-level answer-to-question mapping engine.
 * Level 1: Exact label match
 * Level 2: Normalized label match
 * Level 3: Semantic AI match (Gemini content similarity)
 * Level 4: Contextual match (sequential order for unlabeled answers)
 */
@Service
public class MappingService {

    private static final Logger log = LoggerFactory.getLogger(MappingService.class);

    // Pattern to extract question number and optional sub-part
    private static final Pattern LABEL_PATTERN = Pattern.compile(
            "^[Qq]?\\.?\\s*(\\d+)\\s*[\\(\\-\\.]?\\s*([a-zA-Z])?\\s*[\\)\\.]?\\s*$"
    );

    private final LlmProvider llmProvider;

    public MappingService(LlmProvider llmProvider) {
        this.llmProvider = llmProvider;
    }

    /**
     * Map answers to questions using the four-level strategy.
     * Returns a MappingResult containing mapped pairs, unanswered questions, and unmatched answers.
     */
    public MappingResult mapAnswersToQuestions(
            List<ExtractedQuestion> questions,
            List<ExtractedAnswer> answers) {

        // Build a lookup from normalized question label -> question
        Map<String, ExtractedQuestion> questionByLabel = new HashMap<>();
        for (ExtractedQuestion q : questions) {
            questionByLabel.put(q.getQuestionNumber(), q);
            String normalized = normalizeLabel(q.getQuestionNumber());
            if (normalized != null) {
                questionByLabel.put(normalized, q);
            }
        }

        Set<String> matchedQuestionIds = new HashSet<>();
        Set<String> matchedAnswerIds = new HashSet<>();
        List<MappedQuestionAnswer> mapped = new ArrayList<>();

        // Level 1: Exact label match
        for (ExtractedAnswer answer : answers) {
            if (answer.getDetectedLabel() == null || matchedAnswerIds.contains(answer.getId())) continue;

            String label = answer.getDetectedLabel().trim();
            ExtractedQuestion match = questionByLabel.get(label);
            if (match != null && !matchedQuestionIds.contains(match.getId())) {
                answer.setMappingMethod(MappingMethod.EXPLICIT_LABEL);
                answer.setConfidence(0.95);
                mapped.add(MappedQuestionAnswer.builder()
                        .question(match)
                        .answer(answer)
                        .mappingMethod(MappingMethod.EXPLICIT_LABEL)
                        .confidence(0.95)
                        .build());
                matchedQuestionIds.add(match.getId());
                matchedAnswerIds.add(answer.getId());
                log.debug("L1 exact match: {} -> {}", answer.getDetectedLabel(), match.getQuestionNumber());
            }
        }

        // Level 2: Normalized label match
        for (ExtractedAnswer answer : answers) {
            if (matchedAnswerIds.contains(answer.getId())) continue;
            if (answer.getDetectedLabel() == null) continue;

            String normalized = normalizeLabel(answer.getDetectedLabel());
            if (normalized == null) continue;

            ExtractedQuestion match = questionByLabel.get(normalized);
            if (match != null && !matchedQuestionIds.contains(match.getId())) {
                answer.setMappingMethod(MappingMethod.NORMALIZED_LABEL);
                answer.setNormalizedLabel(normalized);
                answer.setConfidence(0.88);
                mapped.add(MappedQuestionAnswer.builder()
                        .question(match)
                        .answer(answer)
                        .mappingMethod(MappingMethod.NORMALIZED_LABEL)
                        .confidence(0.88)
                        .build());
                matchedQuestionIds.add(match.getId());
                matchedAnswerIds.add(answer.getId());
                log.debug("L2 normalized match: {} ({}) -> {}", answer.getDetectedLabel(), normalized, match.getQuestionNumber());
            }
        }

        // Level 3: Semantic AI match (for answers without matching labels)
        List<ExtractedAnswer> remainingAnswersForAi = answers.stream()
                .filter(a -> !matchedAnswerIds.contains(a.getId()))
                .filter(a -> a.getDetectedLabel() == null || !hasConflictingNumericLabel(a.getDetectedLabel()))
                .collect(Collectors.toList());
        List<ExtractedQuestion> remainingQuestionsForAi = questions.stream()
                .filter(q -> !matchedQuestionIds.contains(q.getId()))
                .collect(Collectors.toList());

        if (!remainingAnswersForAi.isEmpty() && !remainingQuestionsForAi.isEmpty()) {
            try {
                Map<String, String> semanticMatches = llmProvider.semanticMatch(remainingQuestionsForAi, remainingAnswersForAi);
                if (semanticMatches != null) {
                    for (Map.Entry<String, String> entry : semanticMatches.entrySet()) {
                        String answerId = entry.getKey();
                        String questionId = entry.getValue();

                        ExtractedAnswer answer = remainingAnswersForAi.stream()
                                .filter(a -> a.getId().equals(answerId))
                                .findFirst().orElse(null);
                        ExtractedQuestion question = remainingQuestionsForAi.stream()
                                .filter(q -> q.getId().equals(questionId))
                                .findFirst().orElse(null);

                        if (answer != null && question != null
                                && !matchedQuestionIds.contains(question.getId())
                                && !matchedAnswerIds.contains(answer.getId())) {
                            answer.setMappingMethod(MappingMethod.SEMANTIC_AI);
                            answer.setConfidence(0.70);
                            mapped.add(MappedQuestionAnswer.builder()
                                    .question(question)
                                    .answer(answer)
                                    .mappingMethod(MappingMethod.SEMANTIC_AI)
                                    .confidence(0.70)
                                    .build());
                            matchedQuestionIds.add(question.getId());
                            matchedAnswerIds.add(answer.getId());
                            log.debug("L3 semantic match: {} -> {}", answerId, questionId);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Semantic matching failed, skipping: {}", e.getMessage());
            }
        }

        // Level 4: Contextual match (sequential proximity fallback ONLY for unlabeled answers)
        List<ExtractedAnswer> remainingUnlabeledAnswers = answers.stream()
                .filter(a -> !matchedAnswerIds.contains(a.getId()))
                .filter(a -> a.getDetectedLabel() == null)
                .collect(Collectors.toList());
        List<ExtractedQuestion> remainingQuestionsForContext = questions.stream()
                .filter(q -> !matchedQuestionIds.contains(q.getId()))
                .collect(Collectors.toList());

        if (!remainingUnlabeledAnswers.isEmpty() && !remainingQuestionsForContext.isEmpty()) {
            for (int i = 0; i < Math.min(remainingUnlabeledAnswers.size(), remainingQuestionsForContext.size()); i++) {
                ExtractedAnswer answer = remainingUnlabeledAnswers.get(i);
                ExtractedQuestion question = remainingQuestionsForContext.get(i);

                answer.setMappingMethod(MappingMethod.CONTEXTUAL);
                answer.setConfidence(0.60);
                mapped.add(MappedQuestionAnswer.builder()
                        .question(question)
                        .answer(answer)
                        .mappingMethod(MappingMethod.CONTEXTUAL)
                        .confidence(0.60)
                        .build());
                matchedQuestionIds.add(question.getId());
                matchedAnswerIds.add(answer.getId());
                log.debug("L4 contextual match: answer {} -> question {}", answer.getId(), question.getQuestionNumber());
            }
        }

        // Sort mapped questions by display order
        mapped.sort(Comparator.comparingInt(m -> m.getQuestion().getDisplayOrder()));

        // Determine unanswered and unmatched
        List<ExtractedQuestion> unansweredQuestions = questions.stream()
                .filter(q -> !matchedQuestionIds.contains(q.getId()))
                .collect(Collectors.toList());
        List<ExtractedAnswer> unmatchedAnswers = answers.stream()
                .filter(a -> !matchedAnswerIds.contains(a.getId()))
                .collect(Collectors.toList());

        log.info("Mapping complete: {} mapped, {} unanswered, {} unmatched",
                mapped.size(), unansweredQuestions.size(), unmatchedAnswers.size());

        return new MappingResult(mapped, unansweredQuestions, unmatchedAnswers);
    }

    /**
     * Normalize a question/answer label to canonical form.
     * "Q11(a)", "11-a", "11 a", "11.a", "q.11(a)" -> "11(a)"
     * "Q5", "5.", "Ans 5" -> "5"
     */
    public String normalizeLabel(String label) {
        if (label == null) return null;

        // Remove common prefixes (order matters: 'question' and 'answer' must precede 'q' and 'ans')
        String cleaned = label.trim()
                .replaceAll("(?i)^(question|answer|ans|q)\\s*\\.?\\s*", "")
                .replaceAll("^\\.", "")
                .trim();

        Matcher m = LABEL_PATTERN.matcher(cleaned);
        if (m.matches()) {
            String number = m.group(1);
            String subPart = m.group(2);
            if (subPart != null) {
                return number + "(" + subPart.toLowerCase() + ")";
            }
            return number;
        }

        // Also try direct pattern on the cleaned string
        Pattern directPattern = Pattern.compile("^(\\d+)\\s*\\(\\s*([a-zA-Z])\\s*\\)$");
        Matcher dm = directPattern.matcher(cleaned);
        if (dm.matches()) {
            return dm.group(1) + "(" + dm.group(2).toLowerCase() + ")";
        }

        // Try just a number
        if (cleaned.matches("\\d+")) {
            return cleaned;
        }

        return null;
    }

    private boolean hasConflictingNumericLabel(String label) {
        String norm = normalizeLabel(label);
        return norm != null;
    }

    /**
     * Result of the mapping process.
     */
    public record MappingResult(
            List<MappedQuestionAnswer> mapped,
            List<ExtractedQuestion> unanswered,
            List<ExtractedAnswer> unmatched
    ) {}
}
