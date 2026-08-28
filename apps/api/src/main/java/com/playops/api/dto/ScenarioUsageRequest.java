package com.playops.api.dto;

public record ScenarioUsageRequest(
        String nodeType,
        String specPath,
        String grep,
        Boolean enabled
) {}
