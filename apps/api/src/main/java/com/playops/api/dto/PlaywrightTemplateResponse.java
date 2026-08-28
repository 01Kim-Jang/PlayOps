package com.playops.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record PlaywrightTemplateResponse(
        String id,
        String name,
        String description,
        @JsonProperty("default") boolean isDefault,
        List<String> files,
        boolean custom,
        boolean editable,
        boolean deletable
) {}
