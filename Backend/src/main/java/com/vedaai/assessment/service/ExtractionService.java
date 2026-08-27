package com.vedaai.assessment.service;

import com.vedaai.assessment.model.ExtractedAnswer;
import com.vedaai.assessment.model.ExtractedQuestion;
import com.vedaai.assessment.provider.GeminiProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Orchestrates multimodal extraction via Gemini.
 * Sends raw document or page images directly to Gemini for fast extraction + segmentation.
 */
@Service
public class ExtractionService {

    private static final Logger log = LoggerFactory.getLogger(ExtractionService.class);

    private final GeminiProvider geminiProvider;

    public ExtractionService(GeminiProvider geminiProvider) {
        this.geminiProvider = geminiProvider;
    }

    /**
     * Extract and segment questions directly from raw Question Paper bytes (PDF or image).
     * 0ms conversion time — Gemini processes the document directly.
     */
    public List<ExtractedQuestion> extractQuestionsFromDocument(byte[] docBytes, String contentType) throws Exception {
        log.info("Extracting questions directly from raw {} document via Gemini", contentType);
        List<ExtractedQuestion> questions = geminiProvider.extractQuestionsFromDocument(docBytes, contentType);
        log.info("Extracted {} questions directly", questions.size());
        return questions;
    }

    /**
     * Extract and segment questions from question paper page images (fallback / image flow).
     */
    public List<ExtractedQuestion> extractQuestions(List<BufferedImage> pageImages) throws Exception {
        log.info("Extracting questions from {} pages via Gemini multimodal", pageImages.size());
        List<ExtractedQuestion> questions = geminiProvider.extractQuestionsFromImages(pageImages);
        log.info("Extracted {} questions", questions.size());
        return questions;
    }

    /**
     * Extract and segment answers from answer sheet page images.
     * Uses Gemini multimodal to analyze images directly.
     * Returns answers with bounding regions already populated.
     */
    public List<ExtractedAnswer> extractAnswers(List<BufferedImage> pageImages) throws Exception {
        log.info("Extracting answers from {} pages via Gemini multimodal", pageImages.size());
        List<ExtractedAnswer> answers = geminiProvider.extractAnswersFromImages(pageImages);
        log.info("Extracted {} answer blocks", answers.size());
        return answers;
    }
}
