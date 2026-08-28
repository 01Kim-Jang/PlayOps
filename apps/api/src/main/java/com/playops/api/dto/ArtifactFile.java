package com.playops.api.dto;

import java.util.List;

public record ArtifactFile(
        String path,
        String name,
        String type,
        long size
) {}
