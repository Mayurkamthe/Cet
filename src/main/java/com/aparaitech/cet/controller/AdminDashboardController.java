package com.aparaitech.cet.controller;

import com.aparaitech.cet.service.ExamAttemptService;
import com.aparaitech.cet.service.ExamService;
import com.aparaitech.cet.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final UserService userService;
    private final ExamService examService;
    private final ExamAttemptService attemptService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("totalStudents", userService.countStudents());
        model.addAttribute("totalExams", examService.countTotal());
        model.addAttribute("activeExams", examService.countPublished());
        model.addAttribute("draftExams", examService.countDraft());
        model.addAttribute("liveExams", examService.countLive());
        model.addAttribute("upcomingExams", examService.countUpcoming());
        model.addAttribute("completedExams", examService.countCompleted());
        model.addAttribute("recentAttempts", attemptService.getAllAttempts()
            .stream().limit(10).toList());
        return "admin/dashboard";
    }
}
