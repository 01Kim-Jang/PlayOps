package com.playops.api.dto;

import java.util.List;

public record ScaffoldResponse(
        String projectId,
        String templateId,
        List<String> filesCreated,
        boolean skipped
) {}
