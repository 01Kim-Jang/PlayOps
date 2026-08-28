package com.playops.api.dto;

import java.util.List;

public record ScenarioCaseNode(
        String type,
        String title,
        String sourceName,
        int line,
        String grep,
        boolean enabled,
        List<ScenarioCaseNode> children
) {}
