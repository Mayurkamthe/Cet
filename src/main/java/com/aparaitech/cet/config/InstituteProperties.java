package com.aparaitech.cet.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Institute branding config — loaded from application.properties under prefix "app.institute".
 *
 * Example:
 *   app.institute.name=Aparaitech Coaching Institute
 *   app.institute.tagline=Excellence in CET / NEET / JEE Preparation
 *   app.institute.logoUrl=/images/logo.png
 *   app.institute.logoText=AI
 *   app.institute.address=Baramati, Pune, Maharashtra
 *   app.institute.phone=+91 98765 43210
 *   app.institute.email=info@aparaitech.com
 */
@Component
@ConfigurationProperties(prefix = "app.institute")
@Data
public class InstituteProperties {

    /** Full institute name shown in sidebar, login, and page titles */
    private String name = "CET Portal";

    /** Short tagline shown below the name on login page */
    private String tagline = "Mock Test System for Coaching Institutes";

    /**
     * URL of the institute logo image.
     * Place file in src/main/resources/static/images/ and set e.g. /images/logo.png
     * Leave empty to show the default SVG icon.
     */
    private String logoUrl = "";

    /**
     * 1–3 letter abbreviation shown inside the logo placeholder when logoUrl is empty.
     * e.g. "AI" for Aparaitech Institute
     */
    private String logoText = "CP";

    /** Optional address shown in footer */
    private String address = "";

    /** Optional contact phone */
    private String phone = "";

    /** Optional contact email */
    private String email = "";

    /** Copyright year / owner text in footer */
    private String copyright = "Aparaitech Software";
}
