package com.aparaitech.cet.repository;

import com.aparaitech.cet.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = 'ROLE_STUDENT' " +
           "AND (:search IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%',:search,'%')) " +
           "OR LOWER(u.username) LIKE LOWER(CONCAT('%',:search,'%')))")
    Page<User> findAllStudents(@Param("search") String search, Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.name = 'ROLE_STUDENT'")
    long countStudents();
}
