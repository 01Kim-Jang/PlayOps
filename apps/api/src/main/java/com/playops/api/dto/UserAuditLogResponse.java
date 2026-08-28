package com.playops.api.dto;

import com.playops.api.entity.UserAuditAction;
import com.playops.api.entity.UserAuditLog;

import java.time.Instant;

public record UserAuditLogResponse(
        Long id,
        String actorUsername,
        UserAuditAction action,
        String targetUsername,
        String detail,
        Instant createdAt
) {
    public static UserAuditLogResponse from(UserAuditLog log) {
        return new UserAuditLogResponse(
                log.getId(),
                log.getActorUsername(),
                log.getAction(),
                log.getTargetUsername(),
                log.getDetail(),
                log.getCreatedAt()
        );
    }
}
