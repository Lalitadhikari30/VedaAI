package com.vedaai.assessment.service;

import com.vedaai.assessment.dto.AssessmentResultResponse;
import com.vedaai.assessment.model.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Maps internal domain objects to the API response contract.
 */
@Component
public class ResultMapper {

    public AssessmentResultResponse toResponse(AssessmentSession session) {
        List<AssessmentResultResponse.QuestionDto> questionDtos = new ArrayList<>();

        if (session.getMappedQuestions() != null) {
            for (MappedQuestionAnswer mqa : session.getMappedQuestions()) {
                ExtractedQuestion q = mqa.getQuestion();
                ExtractedAnswer a = mqa.getAnswer();
                GradingResult g = mqa.getGrading();

                AssessmentResultResponse.AnswerDto answerDto = null;
                if (a != null) {
                    answerDto = AssessmentResultResponse.AnswerDto.builder()
                            .id(a.getId())
                            .text(a.getText())
                            .confidence(mqa.getConfidence())
                            .mappingMethod(mqa.getMappingMethod() != null ? mqa.getMappingMethod().name() : null)
                            .regions(mapRegions(a.getRegions()))
                            .build();
                }

                AssessmentResultResponse.GradingDto gradingDto = null;
                if (g != null) {
                    gradingDto = AssessmentResultResponse.GradingDto.builder()
                            .score(g.getScore())
                            .maxScore(g.getMaxScore())
                            .status(g.getStatus() != null ? g.getStatus().name() : null)
                            .feedback(g.getFeedback())
                            .build();
                }

                questionDtos.add(AssessmentResultResponse.QuestionDto.builder()
                        .id(q.getId())
                        .number(q.getQuestionNumber())
                        .parentNumber(q.getParentNumber())
                        .subPart(q.getSubPart())
                        .displayLabel(q.getDisplayLabel())
                        .text(q.getText())
                        .displayOrder(q.getDisplayOrder())
                        .answer(answerDto)
                        .grading(gradingDto)
                        .build());
            }
        }

        List<AssessmentResultResponse.UnansweredQuestionDto> unanswered = new ArrayList<>();
        if (session.getUnansweredQuestions() != null) {
            unanswered = session.getUnansweredQuestions().stream()
                    .map(q -> AssessmentResultResponse.UnansweredQuestionDto.builder()
                            .id(q.getId())
                            .number(q.getQuestionNumber())
                            .text(q.getText())
                            .displayOrder(q.getDisplayOrder())
                            .build())
                    .collect(Collectors.toList());
        }

        List<AssessmentResultResponse.UnmatchedAnswerDto> unmatched = new ArrayList<>();
        if (session.getUnmatchedAnswers() != null) {
            unmatched = session.getUnmatchedAnswers().stream()
                    .map(a -> AssessmentResultResponse.UnmatchedAnswerDto.builder()
                            .id(a.getId())
                            .detectedLabel(a.getDetectedLabel())
                            .text(a.getText())
                            .regions(mapRegions(a.getRegions()))
                            .build())
                    .collect(Collectors.toList());
        }

        AssessmentSummary s = session.getSummary();
        AssessmentResultResponse.SummaryDto summaryDto = null;
        if (s != null) {
            summaryDto = AssessmentResultResponse.SummaryDto.builder()
                    .totalQuestions(s.getTotalQuestions())
                    .answered(s.getAnswered())
                    .unanswered(s.getUnanswered())
                    .unmatchedAnswers(s.getUnmatchedAnswers())
                    .totalMarks(s.getTotalMarks())
                    .obtainedMarks(s.getObtainedMarks())
                    .build();
        }

        return AssessmentResultResponse.builder()
                .assessmentId(session.getAssessmentId())
                .status(session.getStatus().name())
                .summary(summaryDto)
                .questions(questionDtos)
                .unansweredQuestions(unanswered)
                .unmatchedAnswers(unmatched)
                .build();
    }

    private List<AssessmentResultResponse.RegionDto> mapRegions(List<AnswerRegion> regions) {
        if (regions == null) return List.of();
        return regions.stream()
                .map(r -> AssessmentResultResponse.RegionDto.builder()
                        .page(r.getPage())
                        .x(r.getX())
                        .y(r.getY())
                        .width(r.getWidth())
                        .height(r.getHeight())
                        .build())
                .collect(Collectors.toList());
    }
}
