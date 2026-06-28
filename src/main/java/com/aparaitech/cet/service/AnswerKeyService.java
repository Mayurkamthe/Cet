package com.aparaitech.cet.service;

import com.aparaitech.cet.entity.AnswerKey;
import com.aparaitech.cet.entity.Exam;
import com.aparaitech.cet.repository.AnswerKeyRepository;
import com.aparaitech.cet.repository.ExamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
