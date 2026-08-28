package com.playops.api.dto;

import java.util.List;

public record ScenarioTreeResponse(
        String projectId,
        int totalSpecs,
        int totalCases,
        List<ScenarioSpecFile> specs
) {}
