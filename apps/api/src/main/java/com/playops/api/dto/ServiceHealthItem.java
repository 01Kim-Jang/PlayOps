package com.playops.api.dto;

public record ServiceHealthItem(
        String id,
        String name,
        String status,
        String target,
        Long latencyMs,
        String message
) {}
