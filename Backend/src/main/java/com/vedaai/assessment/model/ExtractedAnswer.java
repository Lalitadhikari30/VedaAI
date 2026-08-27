package com.vedaai.assessment.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * An answer block extracted from the answer sheet.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractedAnswer {
    private String id;
    private String detectedLabel;    // e.g. "Q2", "11(a)", or null if no label found
    private String normalizedLabel;  // normalized form for matching
    private String text;
    private List<String> wordIds;    // OCR word IDs from answer sheet
    private List<AnswerRegion> regions;  // computed from word IDs
    private double confidence;
    private MappingMethod mappingMethod;
}
