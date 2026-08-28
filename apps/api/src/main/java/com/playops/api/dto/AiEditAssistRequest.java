package com.playops.api.dto;

public record AiEditAssistRequest(String filePath, String currentContent, String instruction) {}
