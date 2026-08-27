package com.vedaai.assessment.provider;

import com.vedaai.assessment.model.ExtractedAnswer;
import com.vedaai.assessment.model.ExtractedQuestion;
import com.vedaai.assessment.model.GradingResult;

import java.util.List;
import java.util.Map;

/**
 * LLM provider interface — handles text/semantic reasoning only, never coordinates.
 */
public interface LlmProvider {

    /**
     * Given a flat word list (id + text, no coords), segment into questions in printed order.
     * Sub-parts (e.g. 11(a), 11(b)) should be separate entries.
     */
    List<ExtractedQuestion> segmentQuestions(List<WordEntry> words) throws Exception;

    /**
     * Given a flat word list from the answer sheet, segment into answer blocks.
     * Detect visible labels (e.g. "Q2", "11(a)") if present.
     */
    List<ExtractedAnswer> segmentAnswers(List<WordEntry> words) throws Exception;

    /**
     * Semantic matching for answers that couldn't be matched by label.
     * Returns map of answerId -> questionId for matched pairs.
     */
    Map<String, String> semanticMatch(List<ExtractedQuestion> questions,
                                       List<ExtractedAnswer> unmatchedAnswers) throws Exception;

    /**
     * Grade a single question-answer pair.
     */
    GradingResult gradeAnswer(String questionText, String answerText, int maxScore) throws Exception;

    /**
     * Batch grade multiple question-answer pairs in a single LLM request.
     * Returns a map of questionId -> GradingResult.
     */
    Map<String, GradingResult> gradeBatch(List<GradingItem> items) throws Exception;

    record GradingItem(String questionId, String questionText, String answerText, int maxScore) {}

    /**
     * A word entry sent to the LLM — just id + text + page, never coordinates.
     */
    record WordEntry(String id, String text, int page) {}
}
