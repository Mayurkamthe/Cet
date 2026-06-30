package com.aparaitech.cet.repository;

import com.aparaitech.cet.entity.Exam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExamRepository extends JpaRepository<Exam, Long> {

    List<Exam> findByStatus(Exam.ExamStatus status);

    @Query("SELECT e FROM Exam e WHERE :search IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%',:search,'%'))")
    Page<Exam> findAllWithSearch(@Param("search") String search, Pageable pageable);

    long countByStatus(Exam.ExamStatus status);

    @Query("SELECT COUNT(e) FROM Exam e WHERE e.status = :status " +
           "AND (e.startAt IS NULL OR e.startAt <= :now) " +
           "AND (e.endAt IS NULL OR e.endAt >= :now)")
    long countLive(@Param("now") java.time.LocalDateTime now, @Param("status") Exam.ExamStatus status);

    @Query("SELECT COUNT(e) FROM Exam e WHERE e.status = :status " +
           "AND e.startAt IS NOT NULL AND e.startAt > :now")
    long countUpcoming(@Param("now") java.time.LocalDateTime now, @Param("status") Exam.ExamStatus status);

    @Query("SELECT COUNT(e) FROM Exam e WHERE e.status = :closedStatus " +
           "OR (e.endAt IS NOT NULL AND e.endAt < :now)")
    long countCompleted(@Param("now") java.time.LocalDateTime now, @Param("closedStatus") Exam.ExamStatus closedStatus);
}
