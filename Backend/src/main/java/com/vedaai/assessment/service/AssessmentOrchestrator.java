package com.vedaai.assessment.service;

import com.vedaai.assessment.model.*;
import com.vedaai.assessment.store.InMemoryAssessmentStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.util.*;

/**
 * Drives the full assessment pipeline as a background task.
 * Updates session status at each stage for frontend polling.
 */
@Service
public class AssessmentOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AssessmentOrchestrator.class);

    private final InMemoryAssessmentStore store;
    private final DocumentService documentService;
    private final ExtractionService extractionService;
    private final MappingService mappingService;
    private final GradingService gradingService;

    public AssessmentOrchestrator(InMemoryAssessmentStore store,
                                   DocumentService documentService,
                                   ExtractionService extractionService,
                                   MappingService mappingService,
                                   GradingService gradingService) {
        this.store = store;
        this.documentService = documentService;
        this.extractionService = extractionService;
        this.mappingService = mappingService;
        this.gradingService = gradingService;
    }

    @Async("assessmentExecutor")
    public void processAssessment(String assessmentId) {
        log.info("Starting assessment processing: {}", assessmentId);

        try {
            AssessmentSession session = store.findById(assessmentId)
                    .orElseThrow(() -> new RuntimeException("Assessment not found: " + assessmentId));

            // Stage 1: Render question paper pages
            updateProgress(assessmentId, "Rendering pages...");
            List<BufferedImage> qpPages = documentService.renderPagesToImages(
                    session.getQuestionPaperBytes(), session.getQuestionPaperContentType());
            session.setQuestionPaperPageCount(qpPages.size());

            // Stage 2: Render answer sheet pages
            List<BufferedImage> asPages = documentService.renderPagesToImages(
                    session.getAnswerSheetBytes(), session.getAnswerSheetContentType());
            session.setAnswerSheetPageCount(asPages.size());

            // Stage 3: Extract questions via Gemini multimodal
            updateProgress(assessmentId, "Extracting questions from question paper...");
            List<ExtractedQuestion> questions = extractionService.extractQuestions(qpPages);
            session.setQuestions(questions);
            session.setQuestionPaperWords(List.of()); // No word-level OCR in multimodal flow
            qpPages.clear(); // Free memory
            log.info("Extracted {} questions", questions.size());

            // Stage 4: Extract answers via Gemini multimodal (includes bounding regions)
            updateProgress(assessmentId, "Extracting answers from answer sheet...");
            List<ExtractedAnswer> answers = extractionService.extractAnswers(asPages);
            session.setAnswers(answers);
            session.setAnswerSheetWords(List.of()); // No word-level OCR in multimodal flow
            asPages.clear(); // Free memory
            log.info("Extracted {} answer blocks with bounding regions", answers.size());

            // Stage 5: Map answers to questions
            updateProgress(assessmentId, "Mapping answers to questions...");
            MappingService.MappingResult mappingResult = mappingService.mapAnswersToQuestions(questions, answers);
            session.setMappedQuestions(mappingResult.mapped());
            session.setUnansweredQuestions(mappingResult.unanswered());
            session.setUnmatchedAnswers(mappingResult.unmatched());

            // Stage 6: Grade (optional, failure-tolerant)
            updateProgress(assessmentId, "Grading answers...");
            try {
                gradingService.gradeAll(mappingResult.mapped());
            } catch (Exception e) {
                log.warn("Grading phase failed but continuing: {}", e.getMessage());
            }

            // Stage 7: Compute summary
            AssessmentSummary summary = gradingService.computeSummary(
                    mappingResult.mapped(), mappingResult.unanswered(), mappingResult.unmatched());
            session.setSummary(summary);

            // Done
            session.setStatus(AssessmentStatus.COMPLETED);
            session.setProgress("Processing complete");
            store.save(session);
            log.info("Assessment {} completed successfully", assessmentId);

        } catch (Exception e) {
            log.error("Assessment {} failed: {}", assessmentId, e.getMessage(), e);
            store.updateStatusWithError(assessmentId, AssessmentStatus.FAILED, e.getMessage());
        }
    }

    private void updateProgress(String assessmentId, String progress) {
        store.updateStatus(assessmentId, AssessmentStatus.PROCESSING, progress);
    }
}
