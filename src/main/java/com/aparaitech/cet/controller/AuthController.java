package com.aparaitech.cet.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController implements ErrorController {

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error,
                        @RequestParam(required = false) String logout,
                        Model model) {
        if (error != null) model.addAttribute("error", "Invalid username or password.");
        if (logout != null) model.addAttribute("message", "You have been logged out.");
        return "auth/login";
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/login";
    }

    /**
     * Catches errors that bypass @ControllerAdvice entirely - 404s, errors from
     * the Spring Security filter chain, static resource failures, etc.
     * Without this explicit /error mapping, Spring Boot's BasicErrorController
     * falls back to the generic Whitelabel Error Page instead of our styled
     * error/general.html template.
     */
    @GetMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute("jakarta.servlet.error.status_code");
        Object message = request.getAttribute("jakarta.servlet.error.message");
        Object exception = request.getAttribute("jakarta.servlet.error.exception");

        String errorMessage;
        if (status != null && status.toString().equals("404")) {
            errorMessage = "Page not found.";
        } else if (exception != null) {
            errorMessage = ((Throwable) exception).getMessage();
        } else if (message != null && !message.toString().isBlank()) {
            errorMessage = message.toString();
        } else {
            errorMessage = "An unexpected error occurred.";
        }

        model.addAttribute("errorMessage", errorMessage);
        return "error/general";
    }
}
