package com.playops.api.llm;

/** 공급자 중립적인 대화 메시지 한 건. */
public record LlmMessage(LlmRole role, String content) {

    public static LlmMessage user(String content) {
        return new LlmMessage(LlmRole.USER, content);
    }

    public static LlmMessage assistant(String content) {
        return new LlmMessage(LlmRole.ASSISTANT, content);
    }
}
