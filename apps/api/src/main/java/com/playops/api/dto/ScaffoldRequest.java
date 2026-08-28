package com.playops.api.dto;

import java.util.List;

public record ScaffoldRequest(
        String templateId,
        Boolean overwrite
) {}
