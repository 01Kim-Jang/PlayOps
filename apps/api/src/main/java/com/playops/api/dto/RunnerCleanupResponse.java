package com.playops.api.dto;

import java.util.List;

public record RunnerCleanupResponse(
        List<String> removed,
        List<String> skipped
) {}
