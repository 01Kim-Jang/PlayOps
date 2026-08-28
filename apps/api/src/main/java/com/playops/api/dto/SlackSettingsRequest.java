package com.playops.api.dto;

public class SlackSettingsRequest {
    private String webhookUrl;

    public String getWebhookUrl() { return webhookUrl; }
    public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }
}
