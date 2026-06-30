package com.aparaitech.cet.config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Automatically adds common attributes to every Thymeleaf model:
 *  - "institute"   -> InstituteProperties bean (${institute.name}, etc.)
 *  - "currentUri"  -> the current request URI, e.g. "/admin/dashboard"
 *
 * currentUri exists because Thymeleaf 3 (used by Spring Boot 3) does NOT
 * expose the legacy #request expression object by default. Templates that
 * try ${#request.requestURI...} throw a TemplateProcessingException with a
 * NullPointerException cause, since #request resolves to null. Injecting
 * currentUri here lets templates safely do ${currentUri.contains('/dashboard')}
 * instead.
 */
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributes {

    private final InstituteProperties institute;

    @ModelAttribute("institute")
    public InstituteProperties institute() {
        return institute;
    }

    @ModelAttribute("currentUri")
    public String currentUri(HttpServletRequest request) {
        return request.getRequestURI();
    }
}
