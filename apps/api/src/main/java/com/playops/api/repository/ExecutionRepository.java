package com.playops.api.repository;

import com.playops.api.entity.Execution;
import com.playops.api.entity.ExecutionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ExecutionRepository extends JpaRepository<Execution, Long> {
    List<Execution> findByProjectIdOrderByCreatedAtDesc(String projectId);

    Optional<Execution> findFirstByProjectIdOrderByCreatedAtDesc(String projectId);

    List<Execution> findByProjectIdAndGrepFilterOrderByCreatedAtDesc(String projectId, String grepFilter);

    boolean existsByProjectIdAndStatusIn(String projectId, Collection<ExecutionStatus> statuses);

    List<Execution> findByStatusIn(Collection<ExecutionStatus> statuses);

    void deleteByProjectId(String projectId);
}
