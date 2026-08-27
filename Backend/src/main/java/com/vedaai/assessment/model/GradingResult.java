package com.vedaai.assessment.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Grading result for a single question-answer pair.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradingResult {
    private int score;
    private int maxScore;
    private GradingStatus status;
    private List<String> conceptsPresent;
    private List<String> conceptsMissing;
    private String feedback;
}
