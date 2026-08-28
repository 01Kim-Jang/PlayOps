package com.playops.api.config;

import com.playops.api.entity.ExecutionStatus;
import com.playops.api.repository.ExecutionRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class ExecutionStartupCleaner implements ApplicationRunner {

    private final ExecutionRepository executionRepository;

    public ExecutionStartupCleaner(ExecutionRepository executionRepository) {
        this.executionRepository = executionRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        var interrupted = executionRepository.findByStatusIn(List.of(
                ExecutionStatus.PENDING,
                ExecutionStatus.RUNNING,
                ExecutionStatus.CANCEL_REQUESTED
        ));
        for (var execution : interrupted) {
            execution.setStatus(ExecutionStatus.ERROR);
            execution.setFinishedAt(Instant.now());
            execution.setErrorMessage("API 서버 재시작 또는 중단으로 실행 상태를 확인할 수 없어 종료 처리했습니다.");
            if (execution.getStartedAt() != null) {
                execution.setDurationMs(execution.getFinishedAt().toEpochMilli() - execution.getStartedAt().toEpochMilli());
            }
        }
        if (!interrupted.isEmpty()) {
            executionRepository.saveAll(interrupted);
        }
    }
}
