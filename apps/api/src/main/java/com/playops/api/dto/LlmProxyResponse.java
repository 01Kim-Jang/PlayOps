package com.playops.api.dto;

public class LlmProxyResponse {
    private String content;

    public LlmProxyResponse() {}
    public LlmProxyResponse(String content) { this.content = content; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
