package com.playops.api.service;

import com.playops.api.dto.AiChatRequest;
import com.playops.api.dto.ExecutionDetailResponse;
import com.playops.api.dto.ExecutionResponse;
import com.playops.api.entity.AiModelProvider;
import com.playops.api.entity.ExecutionStatus;
import com.playops.api.entity.Project;
import com.playops.api.exception.ApiException;
import com.playops.api.llm.LlmMessage;
import com.playops.api.llm.LlmRole;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AiAnalysisServiceTest {

    private final ExecutionQueryService executionQueryService = mock(ExecutionQueryService.class);
    private final ExecutionLogService executionLogService = mock(ExecutionLogService.class);
    private final LlmGatewayService llmGatewayService = mock(LlmGatewayService.class);
    private final ProjectService projectService = mock(ProjectService.class);
    private final AiProviderSettingsService aiProviderSettingsService = mock(AiProviderSettingsService.class);

    private final AiAnalysisService service = new AiAnalysisService(
            executionQueryService,
            executionLogService,
            llmGatewayService,
            projectService,
            aiProviderSettingsService
    );

    @SuppressWarnings("unchecked")
    private static ArgumentCaptor<List<LlmMessage>> messageCaptor() {
        return ArgumentCaptor.forClass((Class<List<LlmMessage>>) (Class<?>) List.class);
    }

    private static AiChatRequest chatRequest(String question) {
        AiChatRequest request = new AiChatRequest();
        request.setQuestion(question);
        return request;
    }

    private static ExecutionDetailResponse detailFor(String projectId) {
        ExecutionResponse execution = new ExecutionResponse(
                7L, projectId, ExecutionStatus.FAILED, null, null, null,
                3, 2, 1, 0, 1200L, null, null, null, null
        );
        return new ExecutionDetailResponse(execution, "", List.of(), null, List.of());
    }

    @Test
    void forwardsHistoryInOrderFollowedByTheNewQuestion() {
        AiChatRequest request = chatRequest("이번엔 왜 실패했나요?");
        request.setProvider("CLAUDE");
        request.setHistory(List.of(
                new AiChatRequest.ChatHistoryMessage("user", "첫 질문"),
                new AiChatRequest.ChatHistoryMessage("assistant", "첫 답변"),
                new AiChatRequest.ChatHistoryMessage("SYSTEM", "무시되어야 함"),
                new AiChatRequest.ChatHistoryMessage("user", "  ")
        ));
        when(llmGatewayService.chat(any(AiModelProvider.class), anyString(), anyList())).thenReturn("답변");

        assertThat(service.chat(request)).isEqualTo("답변");

        ArgumentCaptor<List<LlmMessage>> messages = messageCaptor();
        verify(llmGatewayService).chat(eq(AiModelProvider.CLAUDE), anyString(), messages.capture());
        assertThat(messages.getValue()).containsExactly(
                new LlmMessage(LlmRole.USER, "첫 질문"),
                new LlmMessage(LlmRole.ASSISTANT, "첫 답변"),
                new LlmMessage(LlmRole.USER, "이번엔 왜 실패했나요?")
        );
    }

    @Test
    void dropsLeadingAssistantTurnsSoHistoryStartsWithUser() {
        AiChatRequest request = chatRequest("다음 질문");
        request.setProvider("CLAUDE");
        request.setHistory(List.of(
                new AiChatRequest.ChatHistoryMessage("assistant", "선두 답변"),
                new AiChatRequest.ChatHistoryMessage("user", "질문"),
                new AiChatRequest.ChatHistoryMessage("assistant", "답변")
        ));
        when(llmGatewayService.chat(any(AiModelProvider.class), anyString(), anyList())).thenReturn("ok");

        service.chat(request);

        ArgumentCaptor<List<LlmMessage>> messages = messageCaptor();
        verify(llmGatewayService).chat(eq(AiModelProvider.CLAUDE), anyString(), messages.capture());
        assertThat(messages.getValue()).containsExactly(
                new LlmMessage(LlmRole.USER, "질문"),
                new LlmMessage(LlmRole.ASSISTANT, "답변"),
                new LlmMessage(LlmRole.USER, "다음 질문")
        );
    }

    @Test
    void blankQuestionIsRejectedWithBadRequest() {
        assertThatThrownBy(() -> service.chat(chatRequest("   ")))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(400))
                .hasMessage("질문을 입력해주세요.");

        verifyNoInteractions(llmGatewayService);
    }

    @Test
    void fallsBackToProjectProviderWhenRequestProviderIsBlank() {
        AiChatRequest request = chatRequest("질문");
        request.setExecutionId(7L);

        Project project = new Project();
        project.setAiModelProvider(AiModelProvider.GPT);
        when(executionQueryService.getDetail(7L)).thenReturn(detailFor("demo"));
        when(projectService.getProject("demo")).thenReturn(project);
        when(llmGatewayService.chat(any(AiModelProvider.class), anyString(), anyList())).thenReturn("답변");

        service.chat(request);

        verify(llmGatewayService).chat(eq(AiModelProvider.GPT), anyString(), anyList());
    }

    @Test
    void fallsBackToTheOnlyProviderThatHasAKeyConfigured() {
        when(aiProviderSettingsService.hasKey(AiModelProvider.CLAUDE)).thenReturn(false);
        when(aiProviderSettingsService.hasKey(AiModelProvider.GPT)).thenReturn(true);
        when(llmGatewayService.chat(any(AiModelProvider.class), anyString(), anyList())).thenReturn("답변");

        service.chat(chatRequest("질문"));

        verify(llmGatewayService).chat(eq(AiModelProvider.GPT), anyString(), anyList());
        verifyNoInteractions(projectService);
    }

    @Test
    void unexpectedGatewayFailureBecomesBadGatewayWithoutFakeAnswer() {
        when(llmGatewayService.chat(any(AiModelProvider.class), anyString(), anyList()))
                .thenThrow(new RuntimeException("connection reset"));

        assertThatThrownBy(() -> service.chat(chatRequest("질문")))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(502))
                .hasMessageContaining("connection reset");
    }
}
