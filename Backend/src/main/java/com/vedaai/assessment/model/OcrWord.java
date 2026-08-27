package com.vedaai.assessment.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single word detected by OCR with its bounding box.
 * ID format: "p{page}-w{seq:03d}" e.g. "p2-w014"
 * Coordinates are normalized [0,1] relative to page dimensions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrWord {
    private String id;
    private String text;
    private int page;
    private double x;
    private double y;
    private double width;
    private double height;
    private double confidence;
}
