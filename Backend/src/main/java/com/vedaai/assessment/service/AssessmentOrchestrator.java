package com.vedaai.assessment.service;

import com.vedaai.assessment.model.*;
import com.vedaai.assessment.store.InMemoryAssessmentStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Drives the full assessment pipeline as an optimized parallel background task.
 * Uses 110 DPI fast rasterization and parallel Gemini multimodal execution.
 */
@Service
public class AssessmentOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AssessmentOrchestrator.class);

    private final InMemoryAssessmentStore store;
    private final DocumentService documentService;
    private final ExtractionService extractionService;
    private final MappingService mappingService;
    private final GradingService gradingService;
    private final Executor taskExecutor;

    public AssessmentOrchestrator(InMemoryAssessmentStore store,
            DocumentService documentService,
            ExtractionService extractionService,
            MappingService mappingService,
            GradingService gradingService,
            @Qualifier("assessmentExecutor") Executor taskExecutor) {
        this.store = store;
        this.documentService = documentService;
        this.extractionService = extractionService;
        this.mappingService = mappingService;
        this.gradingService = gradingService;
        this.taskExecutor = taskExecutor;
    }

    @Async("assessmentExecutor")
    public void processAssessment(String assessmentId) {
        log.info("Starting high-speed parallel assessment processing: {}", assessmentId);

        try {
            AssessmentSession session = store.findById(assessmentId)
                    .orElseThrow(() -> new RuntimeException("Assessment not found: " + assessmentId));

            updateProgress(assessmentId, "Extracting questions & answer sheets in parallel...");

            // =========================================================================
            // PARALLEL TASK 1: Question Paper (Fast 110 DPI render + Gemini 4-page JPEG
            // batching)
            // =========================================================================
            CompletableFuture<List<ExtractedQuestion>> qpFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    log.info("[Parallel Thread 1] Extracting Question Paper");
                    int pageCount = documentService.getPageCount(session.getQuestionPaperBytes(), session.getQuestionPaperContentType());
                    session.setQuestionPaperPageCount(pageCount);

                    List<ExtractedQuestion> questions;
                    try {
                        questions = extractionService.extractQuestionsFromDocument(
                                session.getQuestionPaperBytes(), session.getQuestionPaperContentType());
                    } catch (Exception directEx) {
                        log.warn("[Parallel Thread 1] Direct extraction failed, fallback to images: {}", directEx.getMessage());
                        List<BufferedImage> qpPages = documentService.renderPagesToImages(
                                session.getQuestionPaperBytes(), session.getQuestionPaperContentType());
                        questions = extractionService.extractQuestions(qpPages);
                        qpPages.clear();
                    }

                    log.info("[Parallel Thread 1] Extracted {} questions successfully", questions.size());
                    return questions;
                } catch (Exception e) {
                    log.error("[Parallel Thread 1] Question extraction failed: {}", e.getMessage(), e);
                    throw new RuntimeException("Question extraction failed: " + e.getMessage(), e);
                }
            }, taskExecutor);

            // =========================================================================
            // PARALLEL TASK 2: Answer Sheet (Fast 110 DPI render + Bounding Box
            // segmentation)
            // =========================================================================
            CompletableFuture<List<ExtractedAnswer>> asFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    log.info("[Parallel Thread 2] Rendering and extracting Answer Sheet");
                    List<BufferedImage> asPages = documentService.renderPagesToImages(
                            session.getAnswerSheetBytes(), session.getAnswerSheetContentType());
                    session.setAnswerSheetPageCount(asPages.size());

                    List<ExtractedAnswer> answers = extractionService.extractAnswers(asPages);
                    asPages.clear(); // Free memory immediately
                    log.info("[Parallel Thread 2] Extracted {} answer blocks with bounding regions", answers.size());
                    return answers;
                } catch (Exception e) {
                    log.error("[Parallel Thread 2] Answer extraction failed: {}", e.getMessage(), e);
                    throw new RuntimeException("Answer extraction failed: " + e.getMessage(), e);
                }
            }, taskExecutor);

            // =========================================================================
            // WAIT FOR BOTH PARALLEL THREADS TO COMPLETE
            // =========================================================================
            CompletableFuture.allOf(qpFuture, asFuture).join();

            List<ExtractedQuestion> questions = qpFuture.get();
            List<ExtractedAnswer> answers = asFuture.get();

            session.setQuestions(questions);
            session.setQuestionPaperWords(List.of());
            session.setAnswers(answers);
            session.setAnswerSheetWords(List.of());

            // =========================================================================
            // STAGE 3: Map Answers to Questions
            // =========================================================================
            updateProgress(assessmentId, "Mapping answers to questions...");
            MappingService.MappingResult mappingResult = mappingService.mapAnswersToQuestions(questions, answers);
            session.setMappedQuestions(mappingResult.mapped());
            session.setUnansweredQuestions(mappingResult.unanswered());
            session.setUnmatchedAnswers(mappingResult.unmatched());

            // =========================================================================
            // STAGE 4: Grade all mapped answers in batch
            // =========================================================================
            updateProgress(assessmentId, "Grading answers...");
            try {
                gradingService.gradeAll(mappingResult.mapped());
            } catch (Exception e) {
                log.warn("Grading phase failed but continuing: {}", e.getMessage());
            }

            // =========================================================================
            // STAGE 5: Compute summary & Complete
            // =========================================================================
            AssessmentSummary summary = gradingService.computeSummary(
                    mappingResult.mapped(), mappingResult.unanswered(), mappingResult.unmatched());
            session.setSummary(summary);

            session.setStatus(AssessmentStatus.COMPLETED);
            session.setProgress("Processing complete");
            store.save(session);
            log.info("Assessment {} completed successfully in parallel mode!", assessmentId);

        } catch (Exception e) {
            log.error("Assessment {} failed: {}", assessmentId, e.getMessage(), e);
            store.updateStatusWithError(assessmentId, AssessmentStatus.FAILED, e.getMessage());
        }
    }

    private void updateProgress(String assessmentId, String progress) {
        store.updateStatus(assessmentId, AssessmentStatus.PROCESSING, progress);
    }
}
