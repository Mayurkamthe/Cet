package com.aparaitech.cet.controller;

import com.aparaitech.cet.entity.*;
import com.aparaitech.cet.repository.QuestionPaperRepository;
import com.aparaitech.cet.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentController {

    private final ExamService examService;
    private final ExamAttemptService attemptService;
    private final UserService userService;
    private final QuestionPaperRepository questionPaperRepository;

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails ud, Model model) {
        User student = userService.findByUsername(ud.getUsername()).orElseThrow();
        List<ExamAttempt> myAttempts = attemptService.getStudentAttempts(ud.getUsername());
        List<Exam> available = examService.getPublishedExams();

        model.addAttribute("student", student);
        model.addAttribute("available", available);
        model.addAttribute("myAttempts", myAttempts);
        return "student/dashboard";
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal UserDetails ud, Model model) {
        model.addAttribute("student", userService.findByUsername(ud.getUsername()).orElseThrow());
        return "student/profile";
    }

    @GetMapping("/exams")
    public String exams(Model model) {
        model.addAttribute("exams", examService.getPublishedExams());
        return "student/exams";
    }

    /* ---------- CBT Exam Interface ---------- */
    @GetMapping("/exam/{examId}/start")
    public String startExam(@PathVariable Long examId,
                            @AuthenticationPrincipal UserDetails ud,
                            RedirectAttributes ra) {
        try {
            ExamAttempt attempt = attemptService.startExam(examId, ud.getUsername());
            return "redirect:/student/exam/" + attempt.getId() + "/take";
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/student/exams";
        }
    }

    @GetMapping("/exam/{attemptId}/take")
    public String takeExam(@PathVariable Long attemptId,
                           @RequestParam(defaultValue = "1") int q,
                           @AuthenticationPrincipal UserDetails ud,
                           Model model) {
        ExamAttempt attempt = attemptService.findById(attemptId).orElseThrow();

        // Security: only the student who owns this attempt can access
        if (!attempt.getStudent().getUsername().equals(ud.getUsername())) {
            return "redirect:/student/dashboard";
        }

        if (attempt.getStatus() == ExamAttempt.AttemptStatus.SUBMITTED) {
            return "redirect:/student/result/" + attemptId;
        }

        Exam exam = attempt.getExam();
        int totalQ = exam.getTotalQuestions();
        int currentQ = Math.max(1, Math.min(q, totalQ));

        // Get question page image
        QuestionPaper paper = questionPaperRepository.findByExamId(exam.getId()).orElse(null);
        String questionImagePath = null;
        if (paper != null) {
            final int qNum = currentQ;
            questionImagePath = paper.getPages().stream()
                .filter(p -> p.getQuestionNumber().equals(qNum))
                .findFirst()
                .map(QuestionPage::getImagePath)
                .orElse(null);
        }

        // Get all student answers for palette
        List<StudentAnswer> answers = attemptService.getAnswersForAttempt(attemptId);

        // Get current answer for this question
        String currentAnswer = answers.stream()
            .filter(a -> a.getQuestionNumber().equals(currentQ))
            .map(StudentAnswer::getSelectedOption)
            .findFirst().orElse(null);

        boolean currentMarked = answers.stream()
            .filter(a -> a.getQuestionNumber().equals(currentQ))
            .map(StudentAnswer::isMarkedForReview)
            .findFirst().orElse(false);

        // Calculate elapsed seconds
        long elapsedSeconds = 0;
        if (attempt.getStartedAt() != null) {
            elapsedSeconds = java.time.Duration.between(attempt.getStartedAt(), java.time.LocalDateTime.now()).getSeconds();
        }
        long remainingSeconds = (long) exam.getDurationMinutes() * 60 - elapsedSeconds;
        if (remainingSeconds < 0) remainingSeconds = 0;

        model.addAttribute("attempt", attempt);
        model.addAttribute("exam", exam);
        model.addAttribute("currentQ", currentQ);
        model.addAttribute("totalQ", totalQ);
        model.addAttribute("questionImagePath", questionImagePath);
        model.addAttribute("answers", answers);
        model.addAttribute("currentAnswer", currentAnswer);
        model.addAttribute("currentMarked", currentMarked);
        model.addAttribute("remainingSeconds", remainingSeconds);
        return "student/exam-take";
    }

    @PostMapping("/exam/{attemptId}/answer")
    @ResponseBody
    public ResponseEntity<?> saveAnswer(@PathVariable Long attemptId,
                                        @RequestBody Map<String, Object> body,
                                        @AuthenticationPrincipal UserDetails ud) {
        ExamAttempt attempt = attemptService.findById(attemptId).orElse(null);
        if (attempt == null || !attempt.getStudent().getUsername().equals(ud.getUsername())) {
            return ResponseEntity.status(403).body("Forbidden");
        }
        int qNum = (int) body.get("questionNumber");
        String option = (String) body.getOrDefault("selectedOption", null);
        boolean marked = (boolean) body.getOrDefault("markedForReview", false);
        attemptService.saveAnswer(attemptId, qNum, option, marked);
        return ResponseEntity.ok(Map.of("status", "saved"));
    }

    @PostMapping("/exam/{attemptId}/submit")
    public String submitExam(@PathVariable Long attemptId,
                             @AuthenticationPrincipal UserDetails ud,
                             RedirectAttributes ra) {
        ExamAttempt attempt = attemptService.findById(attemptId).orElseThrow();
        if (!attempt.getStudent().getUsername().equals(ud.getUsername())) {
            return "redirect:/student/dashboard";
        }
        attemptService.submitExam(attemptId);
        ra.addFlashAttribute("success", "Exam submitted successfully!");
        return "redirect:/student/result/" + attemptId;
    }

    @GetMapping("/result/{attemptId}")
    public String viewResult(@PathVariable Long attemptId,
                             @AuthenticationPrincipal UserDetails ud,
                             Model model) {
        ExamAttempt attempt = attemptService.findById(attemptId).orElseThrow();
        if (!attempt.getStudent().getUsername().equals(ud.getUsername())) {
            return "redirect:/student/dashboard";
        }
        model.addAttribute("attempt", attempt);
        model.addAttribute("result", attempt.getResult());
        return "student/result";
    }

    @GetMapping("/results")
    public String myResults(@AuthenticationPrincipal UserDetails ud, Model model) {
        model.addAttribute("attempts", attemptService.getStudentAttempts(ud.getUsername())
            .stream().filter(a -> a.getStatus() == ExamAttempt.AttemptStatus.SUBMITTED).toList());
        return "student/results";
    }
}
