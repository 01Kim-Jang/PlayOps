package com.playops.api.dto;

import com.playops.api.entity.ExecutionCaseResult;

public record ExecutionCaseResultResponse(
        String specPath,
        String caseTitle,
        String status,
        Integer durationMs,
        String errorMessage
) {
    public static ExecutionCaseResultResponse from(ExecutionCaseResult r) {
        return new ExecutionCaseResultResponse(
                r.getSpecPath(), r.getCaseTitle(), r.getStatus(), r.getDurationMs(), r.getErrorMessage());
    }
}
