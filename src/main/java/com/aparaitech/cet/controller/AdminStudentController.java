package com.aparaitech.cet.controller;

import com.aparaitech.cet.entity.User;
import com.aparaitech.cet.service.ExcelImportService;
import com.aparaitech.cet.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/students")
@RequiredArgsConstructor
public class AdminStudentController {

    private final UserService userService;
    private final ExcelImportService excelImportService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String search,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        Page<User> students = userService.getAllStudents(
            search.isBlank() ? null : search, page, 10);
        model.addAttribute("students", students);
        model.addAttribute("search", search);
        model.addAttribute("currentPage", page);
        return "admin/students/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        return "admin/students/form";
    }

    @PostMapping("/new")
    public String create(@RequestParam String username,
                         @RequestParam String password,
                         @RequestParam String fullName,
                         @RequestParam String email,
                         @RequestParam(required = false) String phone,
                         RedirectAttributes ra) {
        if (userService.existsByUsername(username)) {
            ra.addFlashAttribute("error", "Username already exists.");
            return "redirect:/admin/students/new";
        }
        if (userService.existsByEmail(email)) {
            ra.addFlashAttribute("error", "Email already exists.");
            return "redirect:/admin/students/new";
        }
        userService.createStudent(username, password, fullName, email, phone);
        ra.addFlashAttribute("success", "Student created successfully.");
        return "redirect:/admin/students";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        User student = userService.findById(id)
            .orElseThrow(() -> new RuntimeException("Student not found"));
        model.addAttribute("student", student);
        return "admin/students/edit";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @RequestParam String fullName,
                         @RequestParam String email,
                         @RequestParam(required = false) String phone,
                         @RequestParam(defaultValue = "false") boolean enabled,
                         RedirectAttributes ra) {
        userService.updateStudent(id, fullName, email, phone, enabled);
        ra.addFlashAttribute("success", "Student updated.");
        return "redirect:/admin/students";
    }

    @PostMapping("/{id}/reset-password")
    public String resetPassword(@PathVariable Long id,
                                @RequestParam String newPassword,
                                RedirectAttributes ra) {
        userService.resetPassword(id, newPassword);
        ra.addFlashAttribute("success", "Password reset successfully.");
        return "redirect:/admin/students";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        userService.deleteStudent(id);
        ra.addFlashAttribute("success", "Student deleted.");
        return "redirect:/admin/students";
    }

    /* ────────────────────────────────────────────
       EXCEL IMPORT
    ──────────────────────────────────────────── */

    @GetMapping("/import")
    public String importPage() {
        return "admin/students/import";
    }

    @PostMapping("/import")
    public String importExcel(@RequestParam("file") MultipartFile file,
                               RedirectAttributes ra) {
        if (file.isEmpty()) {
            ra.addFlashAttribute("error", "Please select an Excel file to upload.");
            return "redirect:/admin/students/import";
        }
        String fileName = file.getOriginalFilename();
        if (fileName == null || (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls"))) {
            ra.addFlashAttribute("error", "Only .xlsx and .xls files are supported.");
            return "redirect:/admin/students/import";
        }
        try {
            ExcelImportService.ImportResult result = excelImportService.importStudents(file);
            String msg = "Import complete: " + result.success + " added, " + result.skipped + " skipped.";
            ra.addFlashAttribute("importSuccess", result.success);
            ra.addFlashAttribute("importSkipped", result.skipped);
            ra.addFlashAttribute("importErrors", result.errors);
            if (result.success > 0) {
                ra.addFlashAttribute("success", msg);
            }
            return "redirect:/admin/students/import";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Import failed: " + e.getMessage());
            return "redirect:/admin/students/import";
        }
    }

    /* ────────────────────────────────────────────
       EXCEL EXPORT
    ──────────────────────────────────────────── */

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportStudents() {
        try {
            List<User> students = userService.getAllStudentsForExport();
            byte[] data = excelImportService.exportStudents(students);
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=students.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /* ────────────────────────────────────────────
       TEMPLATE DOWNLOAD
    ──────────────────────────────────────────── */

    @GetMapping("/import/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        try {
            byte[] data = excelImportService.generateImportTemplate();
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=student_import_template.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
