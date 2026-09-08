package com.playops.api.dto;

public record AiEditAssistVerifyResponse(boolean passed, String log, int durationMs, String specPath) {}
