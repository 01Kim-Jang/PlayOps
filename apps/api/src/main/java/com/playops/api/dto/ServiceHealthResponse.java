package com.playops.api.dto;

import java.time.Instant;
import java.util.List;

public record ServiceHealthResponse(
        String status,
        Instant checkedAt,
        List<ServiceHealthItem> services
) {}
