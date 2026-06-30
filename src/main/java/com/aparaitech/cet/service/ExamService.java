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

    /**
     * Validates that endAt is strictly after startAt when both are provided.
     * Thrown as IllegalArgumentException so the controller can show a
     * friendly flash message instead of a stack trace.
     */
    private void validateSchedule(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt != null && endAt != null && !endAt.isAfter(startAt)) {
            throw new IllegalArgumentException("End date/time must be after the start date/time.");
        }
    }

    public Exam createExam(String title, String description, String examType,
                           Integer durationMinutes, Integer totalQuestions,
                           Integer marksPerCorrect, Integer negativeMarks,
                           LocalDateTime startAt, LocalDateTime endAt) {
        validateSchedule(startAt, endAt);
        Exam exam = Exam.builder()
            .title(title)
            .description(description)
            .examType(examType)
            .durationMinutes(durationMinutes)
            .totalQuestions(totalQuestions)
            .marksPerCorrect(marksPerCorrect != null ? marksPerCorrect : 4)
            .negativeMarks(negativeMarks != null ? negativeMarks : 1)
            .startAt(startAt)
            .endAt(endAt)
            .scheduledAt(startAt) // keep legacy field in sync
            .status(Exam.ExamStatus.DRAFT)
            .build();
        return examRepository.save(exam);
    }

    public Exam updateExam(Long id, String title, String description, String examType,
                           Integer durationMinutes, Integer totalQuestions,
                           Integer marksPerCorrect, Integer negativeMarks,
                           LocalDateTime startAt, LocalDateTime endAt) {
        validateSchedule(startAt, endAt);
        Exam exam = examRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Exam not found"));
        exam.setTitle(title);
        exam.setDescription(description);
        exam.setExamType(examType);
        exam.setDurationMinutes(durationMinutes);
        exam.setTotalQuestions(totalQuestions);
        exam.setMarksPerCorrect(marksPerCorrect);
        exam.setNegativeMarks(negativeMarks);
        exam.setStartAt(startAt);
        exam.setEndAt(endAt);
        exam.setScheduledAt(startAt);
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

    /** Exams whose window has started but not ended — "Live" for dashboard. */
    public long countLive() {
        LocalDateTime now = LocalDateTime.now();
        return examRepository.countLive(now, Exam.ExamStatus.PUBLISHED);
    }

    /** Published exams whose start time is in the future. */
    public long countUpcoming() {
        LocalDateTime now = LocalDateTime.now();
        return examRepository.countUpcoming(now, Exam.ExamStatus.PUBLISHED);
    }

    /** Exams whose end time has passed, or which are explicitly closed. */
    public long countCompleted() {
        LocalDateTime now = LocalDateTime.now();
        return examRepository.countCompleted(now, Exam.ExamStatus.CLOSED);
    }
}
