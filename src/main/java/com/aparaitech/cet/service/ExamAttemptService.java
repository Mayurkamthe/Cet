package com.aparaitech.cet.service;

import com.aparaitech.cet.entity.*;
import com.aparaitech.cet.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ExamAttemptService {

    private final ExamAttemptRepository attemptRepository;
    private final StudentAnswerRepository studentAnswerRepository;
    private final ResultRepository resultRepository;
    private final ExamRepository examRepository;
    private final UserRepository userRepository;
    private final AnswerKeyRepository answerKeyRepository;

    /**
     * Start or resume an exam attempt for a student.
     */
    public ExamAttempt startExam(Long examId, String username) {
        User student = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
        Exam exam = examRepository.findById(examId)
            .orElseThrow(() -> new RuntimeException("Exam not found"));

        if (exam.getStatus() != Exam.ExamStatus.PUBLISHED) {
            throw new RuntimeException("Exam is not available");
        }

        // Check for existing in-progress attempt
        Optional<ExamAttempt> existing = attemptRepository
            .findByExamIdAndStudentIdAndStatus(examId, student.getId(), ExamAttempt.AttemptStatus.IN_PROGRESS);

        if (existing.isPresent()) {
            return existing.get();
        }

        // Check if already submitted
        boolean alreadySubmitted = attemptRepository
            .existsByExamIdAndStudentIdAndStatus(examId, student.getId(), ExamAttempt.AttemptStatus.SUBMITTED);
        if (alreadySubmitted) {
            throw new RuntimeException("You have already submitted this exam.");
        }

        ExamAttempt attempt = ExamAttempt.builder()
            .exam(exam)
            .student(student)
            .status(ExamAttempt.AttemptStatus.IN_PROGRESS)
            .startedAt(LocalDateTime.now())
            .build();

        return attemptRepository.save(attempt);
    }

    /**
     * Save a single answer (called via AJAX as student navigates).
     */
    public void saveAnswer(Long attemptId, Integer questionNumber, String selectedOption, boolean markedForReview) {
        ExamAttempt attempt = attemptRepository.findById(attemptId)
            .orElseThrow(() -> new RuntimeException("Attempt not found"));

        StudentAnswer answer = studentAnswerRepository
            .findByAttemptIdAndQuestionNumber(attemptId, questionNumber)
            .orElse(StudentAnswer.builder().attempt(attempt).questionNumber(questionNumber).build());

        answer.setSelectedOption(selectedOption != null && selectedOption.isBlank() ? null : selectedOption);
        answer.setMarkedForReview(markedForReview);
        studentAnswerRepository.save(answer);
    }

    /**
     * Submit exam and calculate result.
     */
    public Result submitExam(Long attemptId) {
        ExamAttempt attempt = attemptRepository.findById(attemptId)
            .orElseThrow(() -> new RuntimeException("Attempt not found"));

        if (attempt.getStatus() == ExamAttempt.AttemptStatus.SUBMITTED) {
            return resultRepository.findByAttemptId(attemptId)
                .orElseThrow(() -> new RuntimeException("Result not found"));
        }

        attempt.setStatus(ExamAttempt.AttemptStatus.SUBMITTED);
        attempt.setSubmittedAt(LocalDateTime.now());
        attemptRepository.save(attempt);

        return calculateResult(attempt);
    }

    private Result calculateResult(ExamAttempt attempt) {
        Exam exam = attempt.getExam();
        int total = exam.getTotalQuestions();
        int marksPerCorrect = exam.getMarksPerCorrect() != null ? exam.getMarksPerCorrect() : 4;
        int negativeMarks = exam.getNegativeMarks() != null ? exam.getNegativeMarks() : 1;

        List<String> correctAnswers = answerKeyRepository.findByExamId(exam.getId())
            .map(AnswerKey::getAnswerList)
            .orElse(List.of());

        List<StudentAnswer> studentAnswers = studentAnswerRepository.findByAttemptId(attempt.getId());

        int correct = 0, wrong = 0, unanswered = 0, attempted = 0;

        for (int q = 1; q <= total; q++) {
            final int qNum = q;
            Optional<StudentAnswer> sa = studentAnswers.stream()
                .filter(a -> a.getQuestionNumber().equals(qNum))
                .findFirst();

            String selected = sa.map(StudentAnswer::getSelectedOption).orElse(null);
            String correctAns = (q - 1 < correctAnswers.size()) ? correctAnswers.get(q - 1) : null;

            if (selected == null || selected.isBlank()) {
                unanswered++;
            } else {
                attempted++;
                if (correctAns != null && correctAns.equalsIgnoreCase(selected)) {
                    correct++;
                } else {
                    wrong++;
                }
            }
        }

        double rawScore = (correct * marksPerCorrect) - (wrong * negativeMarks);
        double maxScore = total * marksPerCorrect;
        double percentage = maxScore > 0 ? (rawScore / maxScore) * 100 : 0;

        Result result = Result.builder()
            .attempt(attempt)
            .totalQuestions(total)
            .attempted(attempted)
            .correct(correct)
            .wrong(wrong)
            .unanswered(unanswered)
            .rawScore(rawScore)
            .maxScore(maxScore)
            .percentage(Math.round(percentage * 100.0) / 100.0)
            .build();

        return resultRepository.save(result);
    }

    public Optional<ExamAttempt> findById(Long id) {
        return attemptRepository.findById(id);
    }

    public List<ExamAttempt> getStudentAttempts(String username) {
        return userRepository.findByUsername(username)
            .map(u -> attemptRepository.findByStudentId(u.getId()))
            .orElse(List.of());
    }

    public List<StudentAnswer> getAnswersForAttempt(Long attemptId) {
        return studentAnswerRepository.findByAttemptId(attemptId);
    }

    public List<ExamAttempt> getAllAttempts() {
        return attemptRepository.findAll();
    }

    public List<ExamAttempt> getAttemptsByExam(Long examId) {
        return attemptRepository.findByExamId(examId);
    }
}
