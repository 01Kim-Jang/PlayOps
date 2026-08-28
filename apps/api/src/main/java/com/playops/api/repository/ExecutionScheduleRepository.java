package com.playops.api.repository;

import com.playops.api.entity.ExecutionSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExecutionScheduleRepository extends JpaRepository<ExecutionSchedule, Long> {
    List<ExecutionSchedule> findByProjectIdOrderByIdAsc(String projectId);

    List<ExecutionSchedule> findByEnabledTrue();
}
