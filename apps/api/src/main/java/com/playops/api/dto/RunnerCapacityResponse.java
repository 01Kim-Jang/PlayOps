package com.playops.api.dto;

public record RunnerCapacityResponse(
        Boolean autoScaleEnabled,
        Integer baseConcurrency,
        Integer maxConcurrency,
        Integer effectiveMaxConcurrency,
        Integer queueCapacity,
        Integer scaleDownIdleSeconds,
        Integer poolSize,
        Integer activeCount,
        Integer queuedCount,
        Integer remainingQueueCapacity,
        Long completedTaskCount,
        Long taskCount,
        Integer largestPoolSize,
        String scaleDownRule
) {}
