package com.playops.api.dto;

public class AiProviderSettingsRequest {
    private String claudeApiKey;
    private String openaiApiKey;
    private String claudeWorkspaceId;

    public String getClaudeApiKey() { return claudeApiKey; }
    public void setClaudeApiKey(String claudeApiKey) { this.claudeApiKey = claudeApiKey; }

    public String getOpenaiApiKey() { return openaiApiKey; }
    public void setOpenaiApiKey(String openaiApiKey) { this.openaiApiKey = openaiApiKey; }

    public String getClaudeWorkspaceId() { return claudeWorkspaceId; }
    public void setClaudeWorkspaceId(String claudeWorkspaceId) { this.claudeWorkspaceId = claudeWorkspaceId; }
}
