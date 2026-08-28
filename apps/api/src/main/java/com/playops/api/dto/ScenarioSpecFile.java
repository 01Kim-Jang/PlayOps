package com.playops.api.dto;

import java.util.List;

public record ScenarioSpecFile(
        String path,
        int caseCount,
        boolean enabled,
        List<ScenarioCaseNode> cases
) {}
