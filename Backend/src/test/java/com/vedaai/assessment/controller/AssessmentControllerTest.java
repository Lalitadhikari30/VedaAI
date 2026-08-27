package com.vedaai.assessment.controller;

import com.vedaai.assessment.model.AssessmentSession;
import com.vedaai.assessment.model.AssessmentStatus;
import com.vedaai.assessment.service.AssessmentOrchestrator;
import com.vedaai.assessment.service.ResultMapper;
import com.vedaai.assessment.store.InMemoryAssessmentStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AssessmentController.class)
class AssessmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InMemoryAssessmentStore store;

    @MockBean
    private AssessmentOrchestrator orchestrator;

    @MockBean
    private ResultMapper resultMapper;

    @Test
    void testUploadReturnsAccepted() throws Exception {
        MockMultipartFile qpFile = new MockMultipartFile(
                "questionPaper", "test_qp.pdf", "application/pdf", "dummy pdf content".getBytes());
        MockMultipartFile asFile = new MockMultipartFile(
                "answerSheet", "test_as.pdf", "application/pdf", "dummy answer sheet".getBytes());

        mockMvc.perform(multipart("/api/v1/assessments")
                        .file(qpFile)
                        .file(asFile))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.assessmentId").isNotEmpty());
    }

    @Test
    void testGetStatusProcessing() throws Exception {
        AssessmentSession session = AssessmentSession.builder()
                .assessmentId("test-123")
                .status(AssessmentStatus.PROCESSING)
                .progress("Extracting questions...")
                .createdAt(Instant.now())
                .build();

        when(store.findById("test-123")).thenReturn(Optional.of(session));

        mockMvc.perform(get("/api/v1/assessments/test-123/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$.progress").value("Extracting questions..."));
    }

    @Test
    void testGetStatusNotFound() throws Exception {
        when(store.findById("invalid-id")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/assessments/invalid-id/status"))
                .andExpect(status().isNotFound());
    }
}
