package com.aparaitech.cet.repository;

import com.aparaitech.cet.entity.AnswerKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnswerKeyRepository extends JpaRepository<AnswerKey, Long> {
    Optional<AnswerKey> findByExamId(Long examId);
}
