package com.vedaai.assessment.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders document pages to images for OCR processing.
 * Optimized with 110 DPI for 2x faster rasterization and lighter payload.
 */
@Service
public class DocumentService {

    private static final float DPI = 110f; // Optimized for high accuracy and fast rendering

    /**
     * Render pages of a document (PDF or image) to BufferedImages.
     *
     * @param fileBytes   raw file bytes
     * @param contentType MIME type (application/pdf, image/jpeg, image/png)
     * @return list of page images (1 for images, N for PDFs)
     */
    public List<BufferedImage> renderPagesToImages(byte[] fileBytes, String contentType) throws IOException {
        if ("application/pdf".equalsIgnoreCase(contentType)) {
            return renderPdfPages(fileBytes);
        } else {
            // Single image file (JPG/PNG)
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(fileBytes));
            if (image == null) {
                throw new IOException("Failed to read image file");
            }
            return List.of(image);
        }
    }

    /**
     * Get the number of pages in a document.
     */
    public int getPageCount(byte[] fileBytes, String contentType) throws IOException {
        if ("application/pdf".equalsIgnoreCase(contentType)) {
            try (PDDocument doc = Loader.loadPDF(fileBytes)) {
                return doc.getNumberOfPages();
            }
        }
        return 1; // images are single page
    }

    private List<BufferedImage> renderPdfPages(byte[] pdfBytes) throws IOException {
        List<BufferedImage> pages = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFRenderer renderer = new PDFRenderer(document);
            int pageCount = document.getNumberOfPages();
            for (int i = 0; i < pageCount; i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, DPI);
                pages.add(image);
            }
        }
        return pages;
    }
}
