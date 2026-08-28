package com.playops.api.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * 관리자가 등록하는 플랫폼 공용 AI 공급자 키. 항상 단일 row(id=1)만 존재한다.
 */
@Entity
@Table(name = "ai_provider_settings")
public class AiProviderSettings {

    @Id
    private Long id = 1L;

    @Column(name = "claude_api_key_encrypted", columnDefinition = "TEXT")
    private String claudeApiKeyEncrypted;

    @Column(name = "openai_api_key_encrypted", columnDefinition = "TEXT")
    private String openaiApiKeyEncrypted;

    @Column(name = "claude_workspace_id")
    private String claudeWorkspaceId;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void onSave() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }

    public String getClaudeApiKeyEncrypted() { return claudeApiKeyEncrypted; }
    public void setClaudeApiKeyEncrypted(String claudeApiKeyEncrypted) { this.claudeApiKeyEncrypted = claudeApiKeyEncrypted; }

    public String getOpenaiApiKeyEncrypted() { return openaiApiKeyEncrypted; }
    public void setOpenaiApiKeyEncrypted(String openaiApiKeyEncrypted) { this.openaiApiKeyEncrypted = openaiApiKeyEncrypted; }

    public String getClaudeWorkspaceId() { return claudeWorkspaceId; }
    public void setClaudeWorkspaceId(String claudeWorkspaceId) { this.claudeWorkspaceId = claudeWorkspaceId; }

    public Instant getUpdatedAt() { return updatedAt; }
}
