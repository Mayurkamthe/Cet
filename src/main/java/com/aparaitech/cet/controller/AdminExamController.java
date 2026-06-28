package com.aparaitech.cet.controller;

import com.aparaitech.cet.entity.*;
import com.aparaitech.cet.repository.QuestionPaperRepository;
import com.aparaitech.cet.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin/exams")
@RequiredArgsConstructor
public class AdminExamController {

    private final ExamService examService;
    private final PdfService pdfService;
    private final AnswerKeyService answerKeyService;
    private final ExamAttemptService attemptService;
    private final QuestionPaperRepository questionPaperRepository;

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String search,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        Page<Exam> exams = examService.getAllExams(search.isBlank() ? null : search, page, 10);
        model.addAttribute("exams", exams);
        model.addAttribute("search", search);
        model.addAttribute("currentPage", page);
        return "admin/exams/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        return "admin/exams/form";
    }

    @PostMapping("/new")
    public String create(@RequestParam String title,
                         @RequestParam(required = false) String description,
                         @RequestParam String examType,
                         @RequestParam Integer durationMinutes,
                         @RequestParam Integer totalQuestions,
                         @RequestParam(defaultValue = "4") Integer marksPerCorrect,
                         @RequestParam(defaultValue = "1") Integer negativeMarks,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime scheduledAt,
                         RedirectAttributes ra) {
        examService.createExam(title, description, examType, durationMinutes,
            totalQuestions, marksPerCorrect, negativeMarks, scheduledAt);
        ra.addFlashAttribute("success", "Exam created successfully.");
        return "redirect:/admin/exams";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Exam exam = examService.findById(id)
            .orElseThrow(() -> new RuntimeException("Exam not found"));
        model.addAttribute("exam", exam);
        return "admin/exams/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @RequestParam String title,
                         @RequestParam(required = false) String description,
                         @RequestParam String examType,
                         @RequestParam Integer durationMinutes,
                         @RequestParam Integer totalQuestions,
                         @RequestParam(defaultValue = "4") Integer marksPerCorrect,
                         @RequestParam(defaultValue = "1") Integer negativeMarks,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime scheduledAt,
                         RedirectAttributes ra) {
        examService.updateExam(id, title, description, examType, durationMinutes,
            totalQuestions, marksPerCorrect, negativeMarks, scheduledAt);
        ra.addFlashAttribute("success", "Exam updated.");
        return "redirect:/admin/exams";
    }

    @PostMapping("/{id}/publish")
    public String publish(@PathVariable Long id, RedirectAttributes ra) {
        examService.publishExam(id);
        ra.addFlashAttribute("success", "Exam published.");
        return "redirect:/admin/exams";
    }

    @PostMapping("/{id}/close")
    public String close(@PathVariable Long id, RedirectAttributes ra) {
        examService.closeExam(id);
        ra.addFlashAttribute("success", "Exam closed.");
        return "redirect:/admin/exams";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        examService.deleteExam(id);
        ra.addFlashAttribute("success", "Exam deleted.");
        return "redirect:/admin/exams";
    }

    /* ---------- PDF Upload ---------- */
    @GetMapping("/{id}/paper")
    public String paperPage(@PathVariable Long id, Model model) {
        Exam exam = examService.findById(id).orElseThrow();
        model.addAttribute("exam", exam);
        questionPaperRepository.findByExamId(id).ifPresent(p -> model.addAttribute("paper", p));
        return "admin/exams/paper";
    }

    @PostMapping("/{id}/paper/upload")
    public String uploadPaper(@PathVariable Long id,
                              @RequestParam("file") MultipartFile file,
                              RedirectAttributes ra) {
        try {
            pdfService.uploadQuestionPaper(id, file);
            ra.addFlashAttribute("success", "PDF uploaded and pages extracted.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Upload failed: " + e.getMessage());
        }
        return "redirect:/admin/exams/" + id + "/paper";
    }

    @PostMapping("/{id}/paper/delete")
    public String deletePaper(@PathVariable Long id, RedirectAttributes ra) {
        pdfService.deleteQuestionPaper(id);
        ra.addFlashAttribute("success", "Question paper deleted.");
        return "redirect:/admin/exams/" + id + "/paper";
    }

    /* ---------- Answer Key ---------- */
    @GetMapping("/{id}/answers")
    public String answersPage(@PathVariable Long id, Model model) {
        Exam exam = examService.findById(id).orElseThrow();
        model.addAttribute("exam", exam);
        answerKeyService.findByExamId(id).ifPresent(k -> model.addAttribute("answerKey", k));

        // Build an empty list sized to totalQuestions for the form
        List<String> answers = new ArrayList<>();
        answerKeyService.findByExamId(id)
            .map(AnswerKey::getAnswerList)
            .ifPresentOrElse(answers::addAll, () -> {
                for (int i = 0; i < exam.getTotalQuestions(); i++) answers.add("");
            });
        // Pad to totalQuestions
        while (answers.size() < exam.getTotalQuestions()) answers.add("");

        model.addAttribute("answers", answers);
        return "admin/exams/answers";
    }

    @PostMapping("/{id}/answers")
    public String saveAnswers(@PathVariable Long id,
                              @RequestParam String answers,
                              RedirectAttributes ra) {
        answerKeyService.saveAnswers(id, answers);
        ra.addFlashAttribute("success", "Answer key saved.");
        return "redirect:/admin/exams/" + id + "/answers";
    }

    /* ---------- Results ---------- */
    @GetMapping("/{id}/results")
    public String examResults(@PathVariable Long id, Model model) {
        Exam exam = examService.findById(id).orElseThrow();
        model.addAttribute("exam", exam);
        model.addAttribute("attempts", attemptService.getAttemptsByExam(id));
        return "admin/exams/results";
    }
}
