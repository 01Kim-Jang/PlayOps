package com.playops.api.service;

import com.playops.api.config.ResizableThreadPoolTaskExecutor;
import com.playops.api.dto.RunnerCapacityRequest;
import com.playops.api.dto.RunnerCapacityResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadPoolExecutor;

@Service
public class RunnerCapacityService {

    private final RunnerCapacitySettingsService settingsService;
    private final ResizableThreadPoolTaskExecutor executor;

    public RunnerCapacityService(
            RunnerCapacitySettingsService settingsService,
            @Qualifier("executionExecutor") ResizableThreadPoolTaskExecutor executor
    ) {
        this.settingsService = settingsService;
        this.executor = executor;
    }

    public RunnerCapacityResponse getCapacity() {
        return toResponse(settingsService.currentSettings());
    }

    public synchronized RunnerCapacityResponse updateCapacity(RunnerCapacityRequest request) {
        RunnerCapacityRequest settings = settingsService.update(request);
        apply(settings);
        return toResponse(settings);
    }

    public void apply(RunnerCapacityRequest settings) {
        int newCore = settings.baseConcurrency();
        int newMax = settingsService.effectiveMaxConcurrency(settings);

        if (newMax < executor.getCorePoolSize()) {
            executor.setCorePoolSize(Math.min(newCore, newMax));
        }
        if (newMax > executor.getMaxPoolSize()) {
            executor.setMaxPoolSize(newMax);
        }

        executor.setCorePoolSize(newCore);
        executor.setMaxPoolSize(newMax);
        executor.setKeepAliveSeconds(settings.scaleDownIdleSeconds());
        executor.setConfiguredQueueCapacity(settings.queueCapacity());
    }

    private RunnerCapacityResponse toResponse(RunnerCapacityRequest settings) {
        ThreadPoolExecutor delegate = executor.getThreadPoolExecutor();
        int queueCapacity = executor.configuredQueueCapacity();
        int queued = delegate.getQueue().size();
        return new RunnerCapacityResponse(
                settings.autoScaleEnabled(),
                settings.baseConcurrency(),
                settings.maxConcurrency(),
                settingsService.effectiveMaxConcurrency(settings),
                queueCapacity,
                settings.scaleDownIdleSeconds(),
                delegate.getPoolSize(),
                delegate.getActiveCount(),
                queued,
                Math.max(0, queueCapacity - queued),
                delegate.getCompletedTaskCount(),
                delegate.getTaskCount(),
                delegate.getLargestPoolSize(),
                scaleDownRule(settings)
        );
    }

    private String scaleDownRule(RunnerCapacityRequest settings) {
        if (!Boolean.TRUE.equals(settings.autoScaleEnabled())) {
            return "오토스케일이 꺼져 있어 항상 기본 동시 실행 수로 고정됩니다.";
        }
        return "기본 동시 실행 수를 초과한 실행 스레드는 유휴 상태가 "
                + settings.scaleDownIdleSeconds()
                + "초 지속되면 정리되어 기본 동시 실행 수로 축소됩니다.";
    }
}
