package com.playops.api.service;

import com.playops.api.entity.AiModelProvider;
import com.playops.api.exception.ApiException;
import com.playops.api.llm.LlmClient;
import com.playops.api.llm.LlmCredentials;
import com.playops.api.llm.LlmMessage;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Claude/GPT 호출을 한 곳으로 모은 게이트웨이. 관리자가 등록한 플랫폼 공용 키만 사용한다 —
 * 호출자가 개별 API 키를 넘길 수 있는 경로는 존재하지 않는다.
 * 실제 HTTP 호출은 공급자별 {@link LlmClient} 구현이 담당하고, 여기서는 자격증명 해석과 라우팅만 한다.
 */
@Service
public class LlmGatewayService {

    private final AiProviderSettingsService aiProviderSettingsService;
    private final Map<AiModelProvider, LlmClient> clients = new EnumMap<>(AiModelProvider.class);

    public LlmGatewayService(List<LlmClient> llmClients, AiProviderSettingsService aiProviderSettingsService) {
        this.aiProviderSettingsService = aiProviderSettingsService;
        for (LlmClient client : llmClients) {
            clients.put(client.provider(), client);
        }
    }

    /** 단일 질문용 단축 호출. 사용자 메시지 한 건으로 변환해 위임한다. */
    public String chat(AiModelProvider provider, String systemPrompt, String userPrompt) {
        return chat(provider, systemPrompt, List.of(LlmMessage.user(userPrompt)));
    }

    public String chat(AiModelProvider provider, String systemPrompt, List<LlmMessage> messages) {
        LlmClient client = clients.get(provider);
        if (client == null) {
            throw new ApiException(400, "지원하지 않는 AI 공급자입니다: " + provider);
        }
        return client.chat(resolveCredentials(provider), systemPrompt, messages);
    }

    private LlmCredentials resolveCredentials(AiModelProvider provider) {
        String apiKey = aiProviderSettingsService.getDecryptedKey(provider);
        String workspaceId = provider == AiModelProvider.CLAUDE
                ? aiProviderSettingsService.getClaudeWorkspaceIdOrNull()
                : null;
        return new LlmCredentials(apiKey, workspaceId);
    }
}
