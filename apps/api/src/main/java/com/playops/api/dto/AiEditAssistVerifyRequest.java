package com.playops.api.dto;

public record AiEditAssistVerifyRequest(String filePath, String proposedContent, String specPath) {}
