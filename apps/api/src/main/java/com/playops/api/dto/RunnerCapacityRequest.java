package com.playops.api.dto;

public record RunnerCapacityRequest(
        Boolean autoScaleEnabled,
        Integer baseConcurrency,
        Integer maxConcurrency,
        Integer queueCapacity,
        Integer scaleDownIdleSeconds
) {}
