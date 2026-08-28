package com.playops.api.llm;

/** LLM 대화 메시지의 화자. 시스템 프롬프트는 별도 파라미터로 전달하므로 여기 포함하지 않는다. */
public enum LlmRole {
    USER,
    ASSISTANT
}
