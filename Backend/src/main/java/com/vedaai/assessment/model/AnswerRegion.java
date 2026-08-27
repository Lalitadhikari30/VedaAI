package com.vedaai.assessment.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A normalized bounding region on a specific page.
 * All coordinates are normalized [0,1] relative to page dimensions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerRegion {
    private int page;
    private double x;
    private double y;
    private double width;
    private double height;
}
