package com.vedaai.assessment.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * A question mapped to its answer with grading.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MappedQuestionAnswer {
    private ExtractedQuestion question;
    private ExtractedAnswer answer;       // null if unanswered
    private GradingResult grading;        // null if grading unavailable
    private MappingMethod mappingMethod;
    private double confidence;
}
