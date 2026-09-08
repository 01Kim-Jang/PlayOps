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

    /** 실행 소요시간 예상치(ETA) 계산용 — 최근 완료된 실행들의 durationMs만 있으면 된다. */
    List<Execution> findTop20ByDurationMsIsNotNullOrderByCreatedAtDesc();

    void deleteByProjectId(String projectId);
}
