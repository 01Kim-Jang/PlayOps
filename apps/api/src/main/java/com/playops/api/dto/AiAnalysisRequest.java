package com.playops.api.dto;

public class AiAnalysisRequest {
    private Long executionId;
    private String userLevel; // NON_DEVELOPER, JUNIOR, SENIOR
    private String additionalContext;
    private String provider; // CLAUDE, GPT (기본 CLAUDE)

    public AiAnalysisRequest() {}

    public AiAnalysisRequest(Long executionId, String userLevel, String additionalContext) {
        this.executionId = executionId;
        this.userLevel = userLevel;
        this.additionalContext = additionalContext;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Long getExecutionId() {
        return executionId;
    }

    public void setExecutionId(Long executionId) {
        this.executionId = executionId;
    }

    public String getUserLevel() {
        return userLevel;
    }

    public void setUserLevel(String userLevel) {
        this.userLevel = userLevel;
    }

    public String getAdditionalContext() {
        return additionalContext;
    }

    public void setAdditionalContext(String additionalContext) {
        this.additionalContext = additionalContext;
    }
}
