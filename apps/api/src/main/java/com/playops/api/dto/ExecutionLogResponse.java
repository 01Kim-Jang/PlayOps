package com.playops.api.dto;

public record ExecutionLogResponse(
        String content,
        long offset,
        boolean finished,
        String status
) {}
