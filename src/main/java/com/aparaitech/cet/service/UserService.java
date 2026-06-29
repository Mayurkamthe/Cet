package com.aparaitech.cet.service;

import com.aparaitech.cet.entity.Role;
import com.aparaitech.cet.entity.User;
import com.aparaitech.cet.repository.RoleRepository;
import com.aparaitech.cet.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public Page<User> getAllStudents(String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("fullName").ascending());
        return userRepository.findAllStudents(search, pageable);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User createStudent(String username, String rawPassword, String fullName,
                              String email, String phone) {
        Role studentRole = roleRepository.findByName("ROLE_STUDENT")
            .orElseThrow(() -> new RuntimeException("Student role not found"));

        User student = User.builder()
            .username(username)
            .password(passwordEncoder.encode(rawPassword))
            .fullName(fullName)
            .email(email)
            .phone(phone)
            .enabled(true)
            .roles(Set.of(studentRole))
            .build();

        return userRepository.save(student);
    }

    public User updateStudent(Long id, String fullName, String email, String phone, boolean enabled) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found"));
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setEnabled(enabled);
        return userRepository.save(user);
    }

    public void resetPassword(Long id, String newRawPassword) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPassword(passwordEncoder.encode(newRawPassword));
        userRepository.save(user);
    }

    public void deleteStudent(Long id) {
        userRepository.deleteById(id);
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public long countStudents() {
        return userRepository.countStudents();
    }

    public List<User> getAllStudentsForExport() {
        return userRepository.findAllStudents(null, org.springframework.data.domain.Pageable.unpaged()).getContent();
    }
}
