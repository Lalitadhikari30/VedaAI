package com.vedaai.assessment.controller;

import com.vedaai.assessment.dto.AssessmentResultResponse;
import com.vedaai.assessment.dto.AssessmentStatusResponse;
import com.vedaai.assessment.dto.AssessmentUploadResponse;
import com.vedaai.assessment.model.AssessmentSession;
import com.vedaai.assessment.model.AssessmentStatus;
import com.vedaai.assessment.service.AssessmentOrchestrator;
import com.vedaai.assessment.service.ResultMapper;
import com.vedaai.assessment.store.InMemoryAssessmentStore;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/assessments")
public class AssessmentController {

    private final InMemoryAssessmentStore store;
    private final AssessmentOrchestrator orchestrator;
    private final ResultMapper resultMapper;

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png");
    private static final long MAX_SIZE = 15 * 1024 * 1024; // 15MB

    public AssessmentController(InMemoryAssessmentStore store,
            AssessmentOrchestrator orchestrator,
            ResultMapper resultMapper) {
        this.store = store;
        this.orchestrator = orchestrator;
        this.resultMapper = resultMapper;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AssessmentUploadResponse> upload(
            @RequestParam("questionPaper") MultipartFile questionPaper,
            @RequestParam("answerSheet") MultipartFile answerSheet) throws IOException {

        validateFile(questionPaper, "Question Paper");
        validateFile(answerSheet, "Answer Sheet");

        String assessmentId = UUID.randomUUID().toString().substring(0, 8);

        AssessmentSession session = AssessmentSession.builder()
                .assessmentId(assessmentId)
                .status(AssessmentStatus.QUEUED)
                .progress("Queued for processing")
                .questionPaperBytes(questionPaper.getBytes())
                .answerSheetBytes(answerSheet.getBytes())
                .questionPaperFilename(questionPaper.getOriginalFilename())
                .answerSheetFilename(answerSheet.getOriginalFilename())
                .questionPaperContentType(questionPaper.getContentType())
                .answerSheetContentType(answerSheet.getContentType())
                .questionPaperSize(questionPaper.getSize())
                .answerSheetSize(answerSheet.getSize())
                .createdAt(Instant.now())
                .build();

        store.save(session);
        orchestrator.processAssessment(assessmentId);

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new AssessmentUploadResponse(assessmentId));
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<AssessmentStatusResponse> getStatus(@PathVariable String id) {
        AssessmentSession session = store.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assessment not found"));

        return ResponseEntity.ok(AssessmentStatusResponse.builder()
                .status(session.getStatus().name())
                .progress(session.getProgress())
                .message(session.getErrorMessage())
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AssessmentResultResponse> getResult(@PathVariable String id) {
        AssessmentSession session = store.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assessment not found"));

        if (session.getStatus() != AssessmentStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Assessment not yet completed");
        }

        return ResponseEntity.ok(resultMapper.toResponse(session));
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<Void> retry(@PathVariable String id) {
        AssessmentSession session = store.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assessment not found"));

        if (session.getStatus() != AssessmentStatus.FAILED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Can only retry failed assessments");
        }

        session.setStatus(AssessmentStatus.QUEUED);
        session.setProgress("Queued for retry");
        session.setErrorMessage(null);
        store.save(session);
        orchestrator.processAssessment(id);

        return ResponseEntity.accepted().build();
    }

    /**
     * Serve uploaded answer sheet pages as images for the frontend PDF viewer.
     */
    @GetMapping("/{id}/answer-sheet")
    public ResponseEntity<byte[]> getAnswerSheet(@PathVariable String id) {
        AssessmentSession session = store.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assessment not found"));

        String contentType = session.getAnswerSheetContentType();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(session.getAnswerSheetBytes());
    }

    private void validateFile(MultipartFile file, String label) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " is required");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " exceeds 15MB limit");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    label + " must be PDF, JPG, or PNG (got: " + file.getContentType() + ")");
        }
    }
}
