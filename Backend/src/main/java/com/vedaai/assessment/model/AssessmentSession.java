package com.vedaai.assessment.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * In-memory domain object for a single assessment processing session.
 * Stored in ConcurrentHashMap, keyed by assessmentId.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentSession {
    private String assessmentId;
    private AssessmentStatus status;
    private String progress;
    private String errorMessage;

    // Uploaded files (stored as byte arrays in memory)
    private byte[] questionPaperBytes;
    private byte[] answerSheetBytes;
    private String questionPaperFilename;
    private String answerSheetFilename;
    private String questionPaperContentType;
    private String answerSheetContentType;
    private int questionPaperPageCount;
    private int answerSheetPageCount;
    private long questionPaperSize;
    private long answerSheetSize;

    // OCR results
    private List<OcrWord> questionPaperWords;
    private List<OcrWord> answerSheetWords;

    // Extraction results
    private List<ExtractedQuestion> questions;
    private List<ExtractedAnswer> answers;

    // Mapping results
    private List<MappedQuestionAnswer> mappedQuestions;
    private List<ExtractedQuestion> unansweredQuestions;
    private List<ExtractedAnswer> unmatchedAnswers;

    // Summary
    private AssessmentSummary summary;

    // Lifecycle
    private Instant createdAt;
}
