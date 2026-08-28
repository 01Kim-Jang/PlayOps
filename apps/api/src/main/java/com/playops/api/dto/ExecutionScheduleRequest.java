package com.playops.api.dto;

public record ExecutionScheduleRequest(
        String name,
        String specPath,
        Integer hour,
        Integer minute,
        String daysOfWeek,
        Boolean enabled
) {}
