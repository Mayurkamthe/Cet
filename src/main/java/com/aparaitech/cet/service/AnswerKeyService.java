package com.aparaitech.cet.service;

import com.aparaitech.cet.entity.AnswerKey;
import com.aparaitech.cet.entity.Exam;
import com.aparaitech.cet.repository.AnswerKeyRepository;
import com.aparaitech.cet.repository.ExamRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AnswerKeyService {

    private final AnswerKeyRepository answerKeyRepository;
    private final ExamRepository examRepository;

    public Optional<AnswerKey> findByExamId(Long examId) {
        return answerKeyRepository.findByExamId(examId);
    }

    /**
     * Save or update answer key from a comma-separated string like "A,B,C,D,A"
     */
    public AnswerKey saveAnswers(Long examId, String commaSeparated) {
        Exam exam = examRepository.findById(examId)
            .orElseThrow(() -> new RuntimeException("Exam not found"));

        AnswerKey key = answerKeyRepository.findByExamId(examId)
            .orElse(AnswerKey.builder().exam(exam).build());

        // Normalize: trim, uppercase
        String normalized = Arrays.stream(commaSeparated.split(","))
            .map(s -> s.trim().toUpperCase())
            .collect(Collectors.joining(","));

        key.setAnswers(normalized);
        return answerKeyRepository.save(key);
    }

    /**
     * Save answers from a list
     */
    public AnswerKey saveAnswerList(Long examId, List<String> answers) {
        return saveAnswers(examId, String.join(",", answers));
    }

    /**
     * Parse and save an answer key uploaded as Excel (.xlsx/.xls) or CSV.
     * Expected format: one answer per row, optionally with a question-number
     * column first (e.g. "1,A" or just "A"). Only the LAST non-blank cell
     * in each row is treated as the answer letter.
     */
    public AnswerKey uploadAnswerKeyFile(Long examId, MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        List<String> answers;

        if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
            answers = parseExcelAnswers(file);
        } else if (fileName.endsWith(".csv")) {
            answers = parseCsvAnswers(file);
        } else {
            throw new IllegalArgumentException("Unsupported file type. Please upload .xlsx, .xls, or .csv");
        }

        if (answers.isEmpty()) {
            throw new IllegalArgumentException("No answers found in the uploaded file.");
        }

        return saveAnswerList(examId, answers);
    }

    private List<String> parseExcelAnswers(MultipartFile file) throws IOException {
        List<String> answers = new ArrayList<>();
        try (InputStream is = file.getInputStream();
             Workbook wb = new XSSFWorkbook(is)) {
            Sheet sheet = wb.getSheetAt(0);
            for (Row row : sheet) {
                String lastValue = lastNonBlankCell(row);
                if (lastValue != null && !lastValue.isBlank()) {
                    // skip an obvious header row like "Q.No" / "Answer"
                    if (answers.isEmpty() && lastValue.matches("(?i)answer|ans|key|option")) continue;
                    answers.add(lastValue.trim().toUpperCase());
                }
            }
        }
        return answers;
    }

    private String lastNonBlankCell(Row row) {
        String value = null;
        for (Cell cell : row) {
            String v = switch (cell.getCellType()) {
                case STRING -> cell.getStringCellValue();
                case NUMERIC -> String.valueOf((int) cell.getNumericCellValue());
                default -> null;
            };
            if (v != null && !v.isBlank()) value = v;
        }
        return value;
    }

    private List<String> parseCsvAnswers(MultipartFile file) throws IOException {
        List<String> answers = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] parts = line.split(",");
                String last = parts[parts.length - 1].trim();
                if (first && last.matches("(?i)answer|ans|key|option")) {
                    first = false;
                    continue;
                }
                first = false;
                if (!last.isBlank()) {
                    answers.add(last.toUpperCase());
                }
            }
        }
        return answers;
    }
}
