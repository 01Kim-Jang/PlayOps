package com.playops.api.dto;

import java.time.Instant;

public class SlackSettingsResponse {
    private boolean configured;
    private String webhookUrlMasked;
    private Instant updatedAt;

    public SlackSettingsResponse() {}

    public SlackSettingsResponse(boolean configured, String webhookUrlMasked, Instant updatedAt) {
        this.configured = configured;
        this.webhookUrlMasked = webhookUrlMasked;
        this.updatedAt = updatedAt;
    }

    public boolean isConfigured() { return configured; }
    public void setConfigured(boolean configured) { this.configured = configured; }

    public String getWebhookUrlMasked() { return webhookUrlMasked; }
    public void setWebhookUrlMasked(String webhookUrlMasked) { this.webhookUrlMasked = webhookUrlMasked; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
