package com.aparaitech.cet.repository;

import com.aparaitech.cet.entity.QuestionPaper;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuestionPaperRepository extends JpaRepository<QuestionPaper, Long> {
    Optional<QuestionPaper> findByExamId(Long examId);
}
