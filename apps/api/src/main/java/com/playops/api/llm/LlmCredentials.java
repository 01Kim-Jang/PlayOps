package com.playops.api.llm;

/**
 * 게이트웨이가 해석해 클라이언트에 넘기는 호출 자격증명.
 *
 * @param apiKey      평문 API 키
 * @param workspaceId 워크스페이스 연결형 Claude 키에만 필요하다. 그 외에는 null.
 */
public record LlmCredentials(String apiKey, String workspaceId) {

    public static LlmCredentials of(String apiKey) {
        return new LlmCredentials(apiKey, null);
    }
}
