package com.playops.api.dto;

import java.util.List;

public record RunnerCleanupRequest(
        List<String> containerNames,
        Boolean onlyUnused
) {}
