package com.playops.api.repository;

import com.playops.api.entity.AiJob;
import com.playops.api.entity.AiJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiJobRepository extends JpaRepository<AiJob, Long> {
    List<AiJob> findByProjectIdOrderByCreatedAtDesc(String projectId);

    List<AiJob> findByStatusOrderByCreatedAtAsc(AiJobStatus status);

    Optional<AiJob> findByIdAndCallbackToken(Long id, String callbackToken);
}
