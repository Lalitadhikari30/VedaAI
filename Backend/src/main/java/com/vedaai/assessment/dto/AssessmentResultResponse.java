package com.vedaai.assessment.dto;

import com.vedaai.assessment.model.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Full assessment result matching the section 8 contract.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentResultResponse {
    private String assessmentId;
    private String status;
    private SummaryDto summary;
    private List<QuestionDto> questions;
    private List<UnansweredQuestionDto> unansweredQuestions;
    private List<UnmatchedAnswerDto> unmatchedAnswers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SummaryDto {
        private int totalQuestions;
        private int answered;
        private int unanswered;
        private int unmatchedAnswers;
        private int totalMarks;
        private int obtainedMarks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionDto {
        private String id;
        private String number;
        private String parentNumber;
        private String subPart;
        private String displayLabel;
        private String text;
        private int displayOrder;
        private AnswerDto answer;
        private GradingDto grading;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnswerDto {
        private String id;
        private String text;
        private double confidence;
        private String mappingMethod;
        private List<RegionDto> regions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegionDto {
        private int page;
        private double x;
        private double y;
        private double width;
        private double height;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GradingDto {
        private int score;
        private int maxScore;
        private String status;
        private String feedback;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnansweredQuestionDto {
        private String id;
        private String number;
        private String text;
        private int displayOrder;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnmatchedAnswerDto {
        private String id;
        private String detectedLabel;
        private String text;
        private List<RegionDto> regions;
    }
}
