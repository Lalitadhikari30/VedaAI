package com.vedaai.assessment.service;

import com.vedaai.assessment.model.*;
import com.vedaai.assessment.provider.LlmProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Grading service — calls Gemini for batch scoring across all questions in 1 call.
 * Failures here never take down extraction/mapping results.
 */
@Service
public class GradingService {

    private static final Logger log = LoggerFactory.getLogger(GradingService.class);

    private final LlmProvider llmProvider;

    public GradingService(LlmProvider llmProvider) {
        this.llmProvider = llmProvider;
    }

    /**
     * Grade all mapped question-answer pairs in a batch.
     */
    public void gradeAll(List<MappedQuestionAnswer> mappedQuestions) {
        List<LlmProvider.GradingItem> itemsToGrade = new ArrayList<>();

        for (MappedQuestionAnswer mqa : mappedQuestions) {
            if (mqa.getAnswer() != null && mqa.getQuestion() != null) {
                itemsToGrade.add(new LlmProvider.GradingItem(
                        mqa.getQuestion().getId(),
                        mqa.getQuestion().getText(),
                        mqa.getAnswer().getText(),
                        mqa.getQuestion().getMaxScore()
                ));
            }
        }

        if (itemsToGrade.isEmpty()) {
            log.info("No answers to grade");
            return;
        }

        log.info("Batch grading {} mapped answers in a single request", itemsToGrade.size());
        Map<String, GradingResult> results = Map.of();
        try {
            results = llmProvider.gradeBatch(itemsToGrade);
        } catch (Exception e) {
            log.warn("Batch grading request failed: {}", e.getMessage());
        }

        // Apply results to mapped questions
        for (MappedQuestionAnswer mqa : mappedQuestions) {
            if (mqa.getAnswer() == null) continue;

            String qId = mqa.getQuestion().getId();
            GradingResult res = results.get(qId);
            if (res == null) res = results.get(qId.toLowerCase());
            if (res == null) res = results.get(qId.replaceFirst("^[Qq]", ""));
            if (res == null && mqa.getQuestion().getQuestionNumber() != null) {
                res = results.get(mqa.getQuestion().getQuestionNumber());
                if (res == null) res = results.get(mqa.getQuestion().getQuestionNumber().toLowerCase());
            }

            if (res != null) {
                // Score bounds validation
                if (res.getScore() > res.getMaxScore()) {
                    res.setScore(res.getMaxScore());
                }
                if (res.getScore() < 0) {
                    res.setScore(0);
                }
                mqa.setGrading(res);
            } else {
                // Fallback default if missing from batch response
                mqa.setGrading(GradingResult.builder()
                        .score(0)
                        .maxScore(mqa.getQuestion().getMaxScore())
                        .status(GradingStatus.REVIEW)
                        .feedback("Grading pending review")
                        .conceptsPresent(List.of())
                        .conceptsMissing(List.of())
                        .build());
            }
        }
        log.info("Grading completed for {} questions", mappedQuestions.size());
    }

    /**
     * Compute summary statistics from graded results.
     */
    public AssessmentSummary computeSummary(List<MappedQuestionAnswer> mapped,
                                             List<ExtractedQuestion> unanswered,
                                             List<ExtractedAnswer> unmatched) {
        int totalQuestions = mapped.size() + unanswered.size();
        int answered = (int) mapped.stream().filter(m -> m.getAnswer() != null).count();
        int totalMarks = 0;
        int obtainedMarks = 0;

        for (MappedQuestionAnswer mqa : mapped) {
            if (mqa.getGrading() != null) {
                totalMarks += mqa.getGrading().getMaxScore();
                obtainedMarks += mqa.getGrading().getScore();
            } else {
                totalMarks += mqa.getQuestion().getMaxScore();
            }
        }
        for (ExtractedQuestion q : unanswered) {
            totalMarks += q.getMaxScore();
        }

        return AssessmentSummary.builder()
                .totalQuestions(totalQuestions)
                .answered(answered)
                .unanswered(unanswered.size())
                .unmatchedAnswers(unmatched.size())
                .totalMarks(totalMarks)
                .obtainedMarks(obtainedMarks)
                .build();
    }
}
