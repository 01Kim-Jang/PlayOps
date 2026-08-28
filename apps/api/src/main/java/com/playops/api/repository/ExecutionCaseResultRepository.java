package com.playops.api.repository;

import com.playops.api.entity.ExecutionCaseResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExecutionCaseResultRepository extends JpaRepository<ExecutionCaseResult, Long> {
    List<ExecutionCaseResult> findByExecutionIdOrderByIdAsc(Long executionId);
}
