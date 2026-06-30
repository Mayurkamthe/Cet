package com.aparaitech.cet.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Institute branding config — loaded from application.properties under prefix "app.institute".
 *
 * NOTE: This class is intentionally NOT annotated with @Component.
 * It is registered as a bean via @EnableConfigurationProperties(InstituteProperties.class)
 * in CetApplication. Adding @Component here as well creates two competing bean
 * definitions for the same type, which Spring may reject at startup (BeanDefinitionStoreException)
 * or resolve inconsistently — causing the @ModelAttribute("institute") in
 * GlobalModelAttributes to fail intermittently across pages.
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
