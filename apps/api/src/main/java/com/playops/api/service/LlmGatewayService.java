package com.playops.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.playops.api.entity.AiModelProvider;
import com.playops.api.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Claude/GPT 호출을 한 곳으로 모은 게이트웨이. 관리자가 등록한 플랫폼 공용 키만 사용한다 —
 * 호출자가 개별 API 키를 넘길 수 있는 경로는 존재하지 않는다.
 */
@Service
public class LlmGatewayService {

    @Value("${anthropic.model:claude-sonnet-5}")
    private String claudeModel;

    @Value("${openai.model:gpt-4o-mini}")
    private String openaiModel;

    private final AiProviderSettingsService aiProviderSettingsService;
    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LlmGatewayService(AiProviderSettingsService aiProviderSettingsService) {
        this.aiProviderSettingsService = aiProviderSettingsService;
    }

    public String chat(AiModelProvider provider, String systemPrompt, String userPrompt) {
        String apiKey = aiProviderSettingsService.getDecryptedKey(provider);
        return switch (provider) {
            case CLAUDE -> callClaude(apiKey, systemPrompt, userPrompt);
            case GPT -> callOpenAi(apiKey, systemPrompt, userPrompt);
        };
    }

    private String callClaude(String apiKey, String systemPrompt, String userPrompt) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", claudeModel);
        body.put("max_tokens", 4096);
        body.put("system", systemPrompt);
        body.put("messages", List.of(Map.of("role", "user", "content", userPrompt)));

        // 워크스페이스에 연결된(identity-linked) API 키는 x-api-key만으로는 인증이 거부되고
        // 어느 워크스페이스로 요청을 보낼지 별도 헤더로 명시해야 한다. 일반 키는 이 값이 없어도 그대로 동작한다.
        String workspaceId = aiProviderSettingsService.getClaudeWorkspaceIdOrNull();

        try {
            String responseJson = restClient.post()
                    .uri("https://api.anthropic.com/v1/messages")
                    .headers(headers -> {
                        headers.set("x-api-key", apiKey);
                        headers.set("anthropic-version", "2023-06-01");
                        if (workspaceId != null && !workspaceId.isBlank()) {
                            headers.set("anthropic-workspace-id", workspaceId);
                        }
                    })
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode contentArray = root.path("content");
            StringBuilder text = new StringBuilder();
            if (contentArray.isArray()) {
                for (JsonNode block : contentArray) {
                    if ("text".equals(block.path("type").asText())) {
                        text.append(block.path("text").asText());
                    }
                }
            }
            return text.toString();
        } catch (Exception e) {
            throw new ApiException(502, "Claude API 호출 실패: " + e.getMessage());
        }
    }

    private String callOpenAi(String apiKey, String systemPrompt, String userPrompt) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userPrompt));

        Map<String, Object> body = new HashMap<>();
        body.put("model", openaiModel);
        body.put("messages", messages);

        try {
            String responseJson = restClient.post()
                    .uri("https://api.openai.com/v1/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseJson);
            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            throw new ApiException(502, "OpenAI API 호출 실패: " + e.getMessage());
        }
    }
}
