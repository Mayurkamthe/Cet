package com.aparaitech.cet.service;

import com.aparaitech.cet.entity.Exam;
import com.aparaitech.cet.repository.ExamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ExamService {

    private final ExamRepository examRepository;

    public Page<Exam> getAllExams(String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return examRepository.findAllWithSearch(search, pageable);
    }

    public List<Exam> getPublishedExams() {
        return examRepository.findByStatus(Exam.ExamStatus.PUBLISHED);
    }

    public Optional<Exam> findById(Long id) {
        return examRepository.findById(id);
    }

    public Exam createExam(String title, String description, String examType,
                           Integer durationMinutes, Integer totalQuestions,
                           Integer marksPerCorrect, Integer negativeMarks,
                           LocalDateTime scheduledAt) {
        Exam exam = Exam.builder()
            .title(title)
            .description(description)
            .examType(examType)
            .durationMinutes(durationMinutes)
            .totalQuestions(totalQuestions)
            .marksPerCorrect(marksPerCorrect != null ? marksPerCorrect : 4)
            .negativeMarks(negativeMarks != null ? negativeMarks : 1)
            .scheduledAt(scheduledAt)
            .status(Exam.ExamStatus.DRAFT)
            .build();
        return examRepository.save(exam);
    }

    public Exam updateExam(Long id, String title, String description, String examType,
                           Integer durationMinutes, Integer totalQuestions,
                           Integer marksPerCorrect, Integer negativeMarks,
                           LocalDateTime scheduledAt) {
        Exam exam = examRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Exam not found"));
        exam.setTitle(title);
        exam.setDescription(description);
        exam.setExamType(examType);
        exam.setDurationMinutes(durationMinutes);
        exam.setTotalQuestions(totalQuestions);
        exam.setMarksPerCorrect(marksPerCorrect);
        exam.setNegativeMarks(negativeMarks);
        exam.setScheduledAt(scheduledAt);
        return examRepository.save(exam);
    }

    public Exam publishExam(Long id) {
        Exam exam = examRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Exam not found"));
        exam.setStatus(Exam.ExamStatus.PUBLISHED);
        return examRepository.save(exam);
    }

    public Exam closeExam(Long id) {
        Exam exam = examRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Exam not found"));
        exam.setStatus(Exam.ExamStatus.CLOSED);
        return examRepository.save(exam);
    }

    public void deleteExam(Long id) {
        examRepository.deleteById(id);
    }

    public long countTotal() { return examRepository.count(); }
    public long countPublished() { return examRepository.countByStatus(Exam.ExamStatus.PUBLISHED); }
    public long countDraft() { return examRepository.countByStatus(Exam.ExamStatus.DRAFT); }
}
