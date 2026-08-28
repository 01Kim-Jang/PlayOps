package com.playops.api.dto;

import com.playops.api.entity.ExecutionSchedule;

import java.time.Instant;

public record ExecutionScheduleResponse(
        Long id,
        String projectId,
        String name,
        String specPath,
        Integer hour,
        Integer minute,
        String daysOfWeek,
        Boolean enabled,
        Instant lastTriggeredAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static ExecutionScheduleResponse from(ExecutionSchedule schedule) {
        return new ExecutionScheduleResponse(
                schedule.getId(),
                schedule.getProjectId(),
                schedule.getName(),
                schedule.getSpecPath(),
                schedule.getHour(),
                schedule.getMinute(),
                schedule.getDaysOfWeek(),
                schedule.getEnabled(),
                schedule.getLastTriggeredAt(),
                schedule.getCreatedAt(),
                schedule.getUpdatedAt()
        );
    }
}
