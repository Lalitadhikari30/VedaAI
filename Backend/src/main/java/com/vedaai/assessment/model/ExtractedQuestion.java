package com.vedaai.assessment.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * A question extracted from the question paper.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractedQuestion {
    private String id;
    private String questionNumber;   // e.g. "11(a)"
    private String parentNumber;     // e.g. "11"
    private String subPart;          // e.g. "a" or null
    private String displayLabel;     // e.g. "11 a."
    private String text;
    private int displayOrder;
    private List<String> wordIds;    // OCR word IDs from question paper
    private int maxScore;
}
