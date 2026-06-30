package com.aparaitech.cet.service;

import com.aparaitech.cet.entity.*;
import com.aparaitech.cet.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PdfService {

    @Value("${app.upload.dir:./src/main/resources/static/uploads}")
    private String uploadDir;

    @Value("${app.pages.dir:./src/main/resources/static/uploads/pages}")
    private String pagesDir;

    private final QuestionPaperRepository questionPaperRepository;
    private final ExamRepository examRepository;
    private final jakarta.persistence.EntityManager entityManager;

    /**
     * Uploads a PDF question paper for an exam, converts each page to PNG,
     * saves them, and stores metadata in the database.
     *
     * Fix for "UNIQUE constraint failed: question_papers.exam_id":
     * The exam_id column has a unique constraint (one paper per exam). When
     * replacing an existing paper, the old row MUST be deleted and flushed
     * to the database BEFORE the new row is inserted, otherwise both rows
     * exist simultaneously within the same transaction and SQLite rejects
     * the insert. We also explicitly clear the cached association on the
     * Exam entity so Hibernate doesn't try to re-attach the deleted child.
     */
    public QuestionPaper uploadQuestionPaper(Long examId, MultipartFile file) throws IOException {
        Exam exam = examRepository.findById(examId)
            .orElseThrow(() -> new RuntimeException("Exam not found"));

        // Delete old paper if exists — delete AND flush before any insert
        questionPaperRepository.findByExamId(examId).ifPresent(old -> {
            deletePages(old);
            exam.setQuestionPaper(null);
            questionPaperRepository.delete(old);
            questionPaperRepository.flush();
            entityManager.flush();
            entityManager.clear();
        });

        // Re-fetch exam after clearing persistence context
        exam = examRepository.findById(examId)
            .orElseThrow(() -> new RuntimeException("Exam not found"));

        // Prepare directories
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath();
        Path pagesPath = Paths.get(pagesDir, "exam_" + examId).toAbsolutePath();
        Files.createDirectories(uploadPath);
        Files.createDirectories(pagesPath);

        // Save PDF
        String storedName = "exam_" + examId + "_" + UUID.randomUUID() + ".pdf";
        Path pdfPath = uploadPath.resolve(storedName);
        file.transferTo(pdfPath.toFile());

        // Render pages to PNG
        List<QuestionPage> pages = new ArrayList<>();
        try (PDDocument doc = Loader.loadPDF(pdfPath.toFile())) {
            PDFRenderer renderer = new PDFRenderer(doc);
            int numPages = doc.getNumberOfPages();

            for (int i = 0; i < numPages; i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, 150, ImageType.RGB);
                String imgName = "q" + (i + 1) + ".png";
                Path imgPath = pagesPath.resolve(imgName);
                ImageIO.write(image, "PNG", imgPath.toFile());

                QuestionPage qp = QuestionPage.builder()
                    .pageNumber(i + 1)
                    .questionNumber(i + 1)
                    .imageFileName(imgName)
                    .imagePath("/uploads/pages/exam_" + examId + "/" + imgName)
                    .build();
                pages.add(qp);
            }

            QuestionPaper paper = QuestionPaper.builder()
                .exam(exam)
                .originalFileName(file.getOriginalFilename())
                .storedFileName(storedName)
                .filePath(pdfPath.toString())
                .totalPages(numPages)
                .build();

            pages.forEach(p -> {
                p.setQuestionPaper(paper);
                paper.getPages().add(p);
            });

            return questionPaperRepository.save(paper);
        }
    }

    private void deletePages(QuestionPaper paper) {
        try {
            Path pagesPath = Paths.get(pagesDir, "exam_" + paper.getExam().getId()).toAbsolutePath();
            if (Files.exists(pagesPath)) {
                Files.walk(pagesPath)
                    .sorted(java.util.Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            }
            if (paper.getFilePath() != null) {
                Files.deleteIfExists(Paths.get(paper.getFilePath()));
            }
        } catch (IOException e) {
            log.warn("Could not delete old pages: {}", e.getMessage());
        }
    }

    public void deleteQuestionPaper(Long examId) {
        questionPaperRepository.findByExamId(examId).ifPresent(paper -> {
            deletePages(paper);
            questionPaperRepository.delete(paper);
        });
    }

    /**
     * Used by the controller to ask "replace existing file?" before uploading,
     * rather than letting a UNIQUE constraint error bubble up to the user.
     */
    public boolean hasQuestionPaper(Long examId) {
        return questionPaperRepository.findByExamId(examId).isPresent();
    }
}
