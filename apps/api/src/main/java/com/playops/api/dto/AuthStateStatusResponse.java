package com.playops.api.dto;

import java.time.Instant;

public record AuthStateStatusResponse(
        boolean configured,
        String loginSetupSpecPath,
        Integer maxAgeMinutes,
        boolean exists,
        Instant updatedAt,
        Instant expiresAt
) {}
