package com.aparaitech.cet.config;

import com.aparaitech.cet.entity.Role;
import com.aparaitech.cet.entity.User;
import com.aparaitech.cet.repository.RoleRepository;
import com.aparaitech.cet.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Create roles if not exist
        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
            .orElseGet(() -> roleRepository.save(new Role(null, "ROLE_ADMIN")));

        Role studentRole = roleRepository.findByName("ROLE_STUDENT")
            .orElseGet(() -> roleRepository.save(new Role(null, "ROLE_STUDENT")));

        // Create default admin if not exists
        if (!userRepository.existsByUsername("admin")) {
            User admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .fullName("Administrator")
                .email("admin@cetportal.com")
                .enabled(true)
                .roles(Set.of(adminRole))
                .build();
            userRepository.save(admin);
            log.info("✅ Default admin created: username=admin, password=admin123");
        }
    }
}
