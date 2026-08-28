package com.playops.api.dto;

public class AiChatRequest {
    private Long executionId;
    private String question;
    private String userLevel;
    private String provider; // CLAUDE, GPT (기본 CLAUDE)

    public AiChatRequest() {}

    public AiChatRequest(Long executionId, String question, String userLevel) {
        this.executionId = executionId;
        this.question = question;
        this.userLevel = userLevel;
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

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getUserLevel() {
        return userLevel;
    }

    public void setUserLevel(String userLevel) {
        this.userLevel = userLevel;
    }
}
