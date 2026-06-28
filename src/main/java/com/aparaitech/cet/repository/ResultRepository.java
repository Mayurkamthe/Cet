package com.aparaitech.cet.repository;

import com.aparaitech.cet.entity.Result;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResultRepository extends JpaRepository<Result, Long> {
    Optional<Result> findByAttemptId(Long attemptId);
}
