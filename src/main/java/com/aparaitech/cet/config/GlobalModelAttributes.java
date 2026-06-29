package com.aparaitech.cet.config;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Automatically adds "institute" to every Thymeleaf model.
 * Templates access it as ${institute.name}, ${institute.logoUrl}, etc.
 */
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributes {

    private final InstituteProperties institute;

    @ModelAttribute("institute")
    public InstituteProperties institute() {
        return institute;
    }
}
