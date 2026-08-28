package com.playops.api.dto;

public record BoardAttachment(
        String name,
        String contentType,
        Long size,
        String dataUrl
) {}
