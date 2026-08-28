package com.playops.api.dto;

import java.time.Instant;

public class AiProviderSettingsResponse {
    private boolean claudeConfigured;
    private String claudeMasked;
    private boolean openaiConfigured;
    private String openaiMasked;
    private String claudeWorkspaceId;
    private Instant updatedAt;

    public AiProviderSettingsResponse() {}

    public AiProviderSettingsResponse(boolean claudeConfigured, String claudeMasked,
                                       boolean openaiConfigured, String openaiMasked,
                                       String claudeWorkspaceId,
                                       Instant updatedAt) {
        this.claudeConfigured = claudeConfigured;
        this.claudeMasked = claudeMasked;
        this.openaiConfigured = openaiConfigured;
        this.openaiMasked = openaiMasked;
        this.claudeWorkspaceId = claudeWorkspaceId;
        this.updatedAt = updatedAt;
    }

    public boolean isClaudeConfigured() { return claudeConfigured; }
    public void setClaudeConfigured(boolean claudeConfigured) { this.claudeConfigured = claudeConfigured; }

    public String getClaudeMasked() { return claudeMasked; }
    public void setClaudeMasked(String claudeMasked) { this.claudeMasked = claudeMasked; }

    public boolean isOpenaiConfigured() { return openaiConfigured; }
    public void setOpenaiConfigured(boolean openaiConfigured) { this.openaiConfigured = openaiConfigured; }

    public String getOpenaiMasked() { return openaiMasked; }
    public void setOpenaiMasked(String openaiMasked) { this.openaiMasked = openaiMasked; }

    public String getClaudeWorkspaceId() { return claudeWorkspaceId; }
    public void setClaudeWorkspaceId(String claudeWorkspaceId) { this.claudeWorkspaceId = claudeWorkspaceId; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
