package com.aparaitech.cet.config;

import com.aparaitech.cet.entity.Role;
import com.aparaitech.cet.entity.User;
import com.aparaitech.cet.repository.RoleRepository;
import com.aparaitech.cet.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Runs once at startup to seed:
 *  - ROLE_ADMIN and ROLE_STUDENT roles
 *  - Default admin user (only if no admin exists yet)
 *
 * Admin credentials are read from application.properties:
 *   app.admin.username / app.admin.password / app.admin.fullName / app.admin.email
 *
 * Change those values before deploying to production.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.password:admin123}")
    private String adminPassword;

    @Value("${app.admin.fullName:Administrator}")
    private String adminFullName;

    @Value("${app.admin.email:admin@cetportal.com}")
    private String adminEmail;

    @Override
    public void run(String... args) {

        // ── 1. Ensure roles exist ────────────────────────────────────────
        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
            .orElseGet(() -> {
                log.info("Creating role: ROLE_ADMIN");
                return roleRepository.save(new Role(null, "ROLE_ADMIN"));
            });

        roleRepository.findByName("ROLE_STUDENT")
            .orElseGet(() -> {
                log.info("Creating role: ROLE_STUDENT");
                return roleRepository.save(new Role(null, "ROLE_STUDENT"));
            });

        // ── 2. Seed admin user only if none exists ───────────────────────
        if (!userRepository.existsByUsername(adminUsername)) {
            User admin = User.builder()
                .username(adminUsername)
                .password(passwordEncoder.encode(adminPassword))
                .fullName(adminFullName)
                .email(adminEmail)
                .enabled(true)
                .roles(Set.of(adminRole))
                .build();
            userRepository.save(admin);
            log.info("✅ Admin user created — username: '{}'  (change password after first login!)", adminUsername);
        } else {
            log.info("✅ Admin user '{}' already exists — skipping seed.", adminUsername);
        }
    }
}
