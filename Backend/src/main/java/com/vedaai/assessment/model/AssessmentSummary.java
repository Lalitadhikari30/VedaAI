package com.vedaai.assessment.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Summary statistics for an assessment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentSummary {
    private int totalQuestions;
    private int answered;
    private int unanswered;
    private int unmatchedAnswers;
    private int totalMarks;
    private int obtainedMarks;
}
