package com.playops.api.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * HTTP 기반 LLM 클라이언트 공통부. 타임아웃이 설정된 RestClient 를 한 번만 만들어 재사용한다.
 * 기본값 없이 호출하면 응답이 오지 않는 동안 요청 스레드가 무한정 점유되므로 반드시 타임아웃을 건다.
 */
abstract class AbstractHttpLlmClient implements LlmClient {

    protected final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${llm.connect-timeout-ms:10000}")
    private int connectTimeoutMs = 10_000;

    @Value("${llm.read-timeout-ms:90000}")
    private int readTimeoutMs = 90_000;

    private volatile RestClient restClient;

    protected RestClient restClient() {
        RestClient local = restClient;
        if (local == null) {
            synchronized (this) {
                local = restClient;
                if (local == null) {
                    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                    factory.setConnectTimeout(connectTimeoutMs);
                    factory.setReadTimeout(readTimeoutMs);
                    local = RestClient.builder().requestFactory(factory).build();
                    restClient = local;
                }
            }
        }
        return local;
    }
}
