package com.playops.api.dto;

import java.util.List;

public record ExecutionDetailResponse(
        ExecutionResponse execution,
        String logOutput,
        List<ArtifactFile> artifacts,
        String htmlReportUrl,
        List<ExecutionCaseResultResponse> caseResults
) {}
