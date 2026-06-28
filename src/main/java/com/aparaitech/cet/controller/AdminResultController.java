package com.aparaitech.cet.controller;

import com.aparaitech.cet.entity.ExamAttempt;
import com.aparaitech.cet.service.ExamAttemptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin/results")
@RequiredArgsConstructor
public class AdminResultController {

    private final ExamAttemptService attemptService;

    @GetMapping
    public String allResults(@RequestParam(defaultValue = "") String search, Model model) {
        List<ExamAttempt> attempts = attemptService.getAllAttempts().stream()
            .filter(a -> a.getStatus() == ExamAttempt.AttemptStatus.SUBMITTED)
            .filter(a -> search.isBlank() ||
                a.getStudent().getFullName().toLowerCase().contains(search.toLowerCase()) ||
                a.getExam().getTitle().toLowerCase().contains(search.toLowerCase()))
            .toList();
        model.addAttribute("attempts", attempts);
        model.addAttribute("search", search);
        return "admin/results/list";
    }

    @GetMapping("/{attemptId}")
    public String resultDetail(@PathVariable Long attemptId, Model model) {
        ExamAttempt attempt = attemptService.findById(attemptId).orElseThrow();
        model.addAttribute("attempt", attempt);
        model.addAttribute("answers", attemptService.getAnswersForAttempt(attemptId));
        return "admin/results/detail";
    }
}
