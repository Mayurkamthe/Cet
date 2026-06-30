package com.aparaitech.cet.controller;

import com.aparaitech.cet.entity.*;
import com.aparaitech.cet.repository.AnswerKeyRepository;
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
    private final AnswerKeyRepository answerKeyRepository;

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
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startAt,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endAt,
                         RedirectAttributes ra) {
        try {
            Exam exam = examService.createExam(title, description, examType, durationMinutes,
                totalQuestions, marksPerCorrect, negativeMarks, startAt, endAt);
            ra.addFlashAttribute("success", "Exam created successfully. Now upload the question paper.");
            // Step 2 of unified workflow: send admin straight to paper upload
            return "redirect:/admin/exams/" + exam.getId() + "/paper";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            ra.addFlashAttribute("formTitle", title);
            ra.addFlashAttribute("formDescription", description);
            ra.addFlashAttribute("formExamType", examType);
            ra.addFlashAttribute("formDuration", durationMinutes);
            ra.addFlashAttribute("formTotalQuestions", totalQuestions);
            ra.addFlashAttribute("formMarksPerCorrect", marksPerCorrect);
            ra.addFlashAttribute("formNegativeMarks", negativeMarks);
            return "redirect:/admin/exams/new";
        }
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
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startAt,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endAt,
                         RedirectAttributes ra) {
        try {
            examService.updateExam(id, title, description, examType, durationMinutes,
                totalQuestions, marksPerCorrect, negativeMarks, startAt, endAt);
            ra.addFlashAttribute("success", "Exam updated successfully.");
            return "redirect:/admin/exams";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/exams/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/publish")
    public String publish(@PathVariable Long id, RedirectAttributes ra) {
        Exam exam = examService.findById(id).orElseThrow(() -> new RuntimeException("Exam not found"));

        // Guard rails: don't allow publishing an incomplete exam (item #7 workflow)
        boolean hasPaper = questionPaperRepository.findByExamId(id).isPresent();
        boolean hasAnswers = answerKeyRepository.findByExamId(id).isPresent();

        if (!hasPaper) {
            ra.addFlashAttribute("error", "Cannot publish: please upload the question paper first.");
            return "redirect:/admin/exams/" + id + "/paper";
        }
        if (!hasAnswers) {
            ra.addFlashAttribute("error", "Cannot publish: please set the answer key first.");
            return "redirect:/admin/exams/" + id + "/answers";
        }

        examService.publishExam(id);
        ra.addFlashAttribute("success", "Exam published successfully. Students can now see it.");
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
                              @RequestParam(defaultValue = "false") boolean confirmReplace,
                              RedirectAttributes ra) {
        if (file == null || file.isEmpty()) {
            ra.addFlashAttribute("error", "Please choose a PDF file to upload.");
            return "redirect:/admin/exams/" + id + "/paper";
        }
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase().endsWith(".pdf")) {
            ra.addFlashAttribute("error", "Only PDF files are supported for the question paper.");
            return "redirect:/admin/exams/" + id + "/paper";
        }

        // Item #6: ask before silently overwriting / erroring on duplicate
        if (pdfService.hasQuestionPaper(id) && !confirmReplace) {
            ra.addFlashAttribute("confirmReplaceNeeded", true);
            ra.addFlashAttribute("error", "A question paper already exists for this exam. Replace it?");
            return "redirect:/admin/exams/" + id + "/paper";
        }

        try {
            pdfService.uploadQuestionPaper(id, file);
            ra.addFlashAttribute("success", "Question Paper Uploaded Successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Could not upload the question paper. Please try again or use a different file.");
        }
        return "redirect:/admin/exams/" + id + "/paper";
    }

    @PostMapping("/{id}/paper/delete")
    public String deletePaper(@PathVariable Long id, RedirectAttributes ra) {
        pdfService.deleteQuestionPaper(id);
        ra.addFlashAttribute("success", "Question paper removed.");
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
        try {
            answerKeyService.saveAnswers(id, answers);
            ra.addFlashAttribute("success", "Answer Key Uploaded Successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Could not save the answer key: " + e.getMessage());
        }
        return "redirect:/admin/exams/" + id + "/answers";
    }

    @PostMapping("/{id}/answers/upload")
    public String uploadAnswerFile(@PathVariable Long id,
                                   @RequestParam("file") MultipartFile file,
                                   RedirectAttributes ra) {
        if (file == null || file.isEmpty()) {
            ra.addFlashAttribute("error", "Please choose a file to upload.");
            return "redirect:/admin/exams/" + id + "/answers";
        }
        try {
            answerKeyService.uploadAnswerKeyFile(id, file);
            ra.addFlashAttribute("success", "Answer Key Uploaded Successfully.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Could not read the uploaded file. Please check the format and try again.");
        }
        return "redirect:/admin/exams/" + id + "/answers";
    }

    /* ---------- Preview (item #4 / #7) ---------- */
    @GetMapping("/{id}/preview")
    public String previewExam(@PathVariable Long id, Model model) {
        Exam exam = examService.findById(id).orElseThrow();
        model.addAttribute("exam", exam);
        questionPaperRepository.findByExamId(id).ifPresent(p -> model.addAttribute("paper", p));
        answerKeyService.findByExamId(id).ifPresent(k -> model.addAttribute("answerKey", k));
        return "admin/exams/preview";
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
