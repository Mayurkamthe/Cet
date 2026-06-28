package com.aparaitech.cet.repository;

import com.aparaitech.cet.entity.ExamAttempt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, Long> {

    Optional<ExamAttempt> findByExamIdAndStudentIdAndStatus(Long examId, Long studentId, ExamAttempt.AttemptStatus status);

    List<ExamAttempt> findByStudentId(Long studentId);

    List<ExamAttempt> findByStudentIdAndStatus(Long studentId, ExamAttempt.AttemptStatus status);

    boolean existsByExamIdAndStudentIdAndStatus(Long examId, Long studentId, ExamAttempt.AttemptStatus status);

    @Query("SELECT a FROM ExamAttempt a WHERE " +
           "(:search IS NULL OR LOWER(a.student.fullName) LIKE LOWER(CONCAT('%',:search,'%')))")
    Page<ExamAttempt> findAllWithSearch(@Param("search") String search, Pageable pageable);

    List<ExamAttempt> findByExamId(Long examId);
}
