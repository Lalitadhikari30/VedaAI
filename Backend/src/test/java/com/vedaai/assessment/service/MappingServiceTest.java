package com.vedaai.assessment.service;

import com.vedaai.assessment.model.*;
import com.vedaai.assessment.provider.LlmProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class MappingServiceTest {

    private LlmProvider llmProvider;
    private MappingService mappingService;

    @BeforeEach
    void setUp() {
        llmProvider = Mockito.mock(LlmProvider.class);
        mappingService = new MappingService(llmProvider);
    }

    @Test
    void testNormalizeLabelVariations() {
        assertEquals("11(a)", mappingService.normalizeLabel("11(a)"));
        assertEquals("11(a)", mappingService.normalizeLabel("11-a"));
        assertEquals("11(a)", mappingService.normalizeLabel("11 a"));
        assertEquals("11(a)", mappingService.normalizeLabel("11.a"));
        assertEquals("11(a)", mappingService.normalizeLabel("Q11(a)"));
        assertEquals("11(a)", mappingService.normalizeLabel("q.11(a)"));
        assertEquals("11(a)", mappingService.normalizeLabel("Question 11(a)"));

        assertEquals("5", mappingService.normalizeLabel("Q5"));
        assertEquals("5", mappingService.normalizeLabel("5."));
        assertEquals("5", mappingService.normalizeLabel("Ans 5"));
        assertEquals("5", mappingService.normalizeLabel("5"));
    }

    @Test
    void testExactAndNormalizedLabelMapping() {
        List<ExtractedQuestion> questions = List.of(
                ExtractedQuestion.builder().id("q1").questionNumber("1").displayOrder(1).maxScore(2).build(),
                ExtractedQuestion.builder().id("q2").questionNumber("2").displayOrder(2).maxScore(2).build(),
                ExtractedQuestion.builder().id("q11a").questionNumber("11(a)").parentNumber("11").subPart("a").displayOrder(11).maxScore(2).build(),
                ExtractedQuestion.builder().id("q11b").questionNumber("11(b)").parentNumber("11").subPart("b").displayOrder(12).maxScore(3).build()
        );

        List<ExtractedAnswer> answers = List.of(
                ExtractedAnswer.builder().id("a1").detectedLabel("1").text("Answer 1").build(),
                ExtractedAnswer.builder().id("a2").detectedLabel("Q2").text("Answer 2").build(),
                ExtractedAnswer.builder().id("a3").detectedLabel("11-a").text("Answer 11a").build(),
                ExtractedAnswer.builder().id("a4").detectedLabel("11(b)").text("Answer 11b").build()
        );

        MappingService.MappingResult result = mappingService.mapAnswersToQuestions(questions, answers);

        assertEquals(4, result.mapped().size());
        assertEquals(0, result.unanswered().size());
        assertEquals(0, result.unmatched().size());

        assertEquals("q1", result.mapped().get(0).getQuestion().getId());
        assertEquals("q2", result.mapped().get(1).getQuestion().getId());
        assertEquals("q11a", result.mapped().get(2).getQuestion().getId());
        assertEquals("q11b", result.mapped().get(3).getQuestion().getId());

        assertEquals(MappingMethod.EXPLICIT_LABEL, result.mapped().get(0).getMappingMethod());
        assertEquals(MappingMethod.NORMALIZED_LABEL, result.mapped().get(1).getMappingMethod());
        assertEquals(MappingMethod.NORMALIZED_LABEL, result.mapped().get(2).getMappingMethod());
        assertEquals(MappingMethod.EXPLICIT_LABEL, result.mapped().get(3).getMappingMethod());
    }

    @Test
    void testUnansweredAndUnmatchedAnswers() {
        List<ExtractedQuestion> questions = List.of(
                ExtractedQuestion.builder().id("q1").questionNumber("1").displayOrder(1).maxScore(2).build(),
                ExtractedQuestion.builder().id("q2").questionNumber("2").displayOrder(2).maxScore(2).build(),
                ExtractedQuestion.builder().id("q3").questionNumber("3").displayOrder(3).maxScore(5).build() // Unanswered
        );

        List<ExtractedAnswer> answers = List.of(
                ExtractedAnswer.builder().id("a1").detectedLabel("1").text("Answer 1").build(),
                ExtractedAnswer.builder().id("a2").detectedLabel("2").text("Answer 2").build(),
                ExtractedAnswer.builder().id("a99").detectedLabel("99").text("Extra answer").build() // Unmatched
        );

        MappingService.MappingResult result = mappingService.mapAnswersToQuestions(questions, answers);

        assertEquals(2, result.mapped().size());
        assertEquals(1, result.unanswered().size());
        assertEquals("q3", result.unanswered().get(0).getId());

        assertEquals(1, result.unmatched().size());
        assertEquals("a99", result.unmatched().get(0).getId());
    }

    @Test
    void testOutOfOrderAnswers() {
        List<ExtractedQuestion> questions = List.of(
                ExtractedQuestion.builder().id("q1").questionNumber("1").displayOrder(1).maxScore(2).build(),
                ExtractedQuestion.builder().id("q2").questionNumber("2").displayOrder(2).maxScore(2).build(),
                ExtractedQuestion.builder().id("q3").questionNumber("3").displayOrder(3).maxScore(2).build()
        );

        // Student wrote answers in order: Q3, Q1, Q2
        List<ExtractedAnswer> answers = List.of(
                ExtractedAnswer.builder().id("a3").detectedLabel("Q3").text("Answer 3").build(),
                ExtractedAnswer.builder().id("a1").detectedLabel("Q1").text("Answer 1").build(),
                ExtractedAnswer.builder().id("a2").detectedLabel("Q2").text("Answer 2").build()
        );

        MappingService.MappingResult result = mappingService.mapAnswersToQuestions(questions, answers);

        assertEquals(3, result.mapped().size());
        // Mapped results should be sorted by question display order (1, 2, 3)
        assertEquals("q1", result.mapped().get(0).getQuestion().getId());
        assertEquals("q2", result.mapped().get(1).getQuestion().getId());
        assertEquals("q3", result.mapped().get(2).getQuestion().getId());
    }

    @Test
    void testSemanticFallback() throws Exception {
        List<ExtractedQuestion> questions = List.of(
                ExtractedQuestion.builder().id("q1").questionNumber("1").text("Explain photosynthesis").displayOrder(1).maxScore(5).build()
        );

        // Answer has no label
        List<ExtractedAnswer> answers = List.of(
                ExtractedAnswer.builder().id("a1").detectedLabel(null).text("Photosynthesis is process where plants convert light into chemical energy").build()
        );

        when(llmProvider.semanticMatch(any(), any())).thenReturn(Map.of("a1", "q1"));

        MappingService.MappingResult result = mappingService.mapAnswersToQuestions(questions, answers);

        assertEquals(1, result.mapped().size());
        assertEquals("q1", result.mapped().get(0).getQuestion().getId());
        assertEquals(MappingMethod.SEMANTIC_AI, result.mapped().get(0).getMappingMethod());
    }
}
