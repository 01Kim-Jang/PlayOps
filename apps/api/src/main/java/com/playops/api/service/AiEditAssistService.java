package com.playops.api.service;

import com.playops.api.entity.Project;
import com.playops.api.exception.ApiException;
import org.springframework.stereotype.Service;

/**
 * 소스 탐색기에서 파일을 편집하는 동안 AI에게 자연어로 수정을 요청하는 기능("Playwright 시나리오 작성
 * AI 도움").파일을 직접 읽거나 쓰지 않는다 — 에디터에 이미 열려 있는 버퍼 내용을 그대로 받아 수정된
 * 전체 내용을 돌려주고, 프론트가 에디터 버퍼만 교체한다. 실제 파일 저장은 기존 "저장" 버튼을 눌러야
 * 반영되므로 그 자체가 검토 단계 역할을 한다 — CODE_FIX/TEMPLATE_GENERATE처럼 별도 AiJob 검토 파이프라인이
 * 필요 없다.
 */
@Service
public class AiEditAssistService {

    private final LlmGatewayService llmGatewayService;
    private final ProjectService projectService;

    public AiEditAssistService(LlmGatewayService llmGatewayService, ProjectService projectService) {
        this.llmGatewayService = llmGatewayService;
        this.projectService = projectService;
    }

    public String editFile(String projectId, String filePath, String currentContent, String instruction) {
        if (filePath == null || filePath.isBlank()) {
            throw new ApiException(400, "대상 파일 경로가 필요합니다.");
        }
        if (instruction == null || instruction.isBlank()) {
            throw new ApiException(400, "요청 사항을 입력하세요.");
        }
        Project project = projectService.getProject(projectId);

        String systemPrompt = """
                You are an expert QA automation engineer specializing in Playwright v%s and TypeScript,
                helping a user edit the file "%s" inside an existing PlayOps-managed test project.
                You will be given the file's CURRENT full content (may be empty) and a natural-language instruction.
                Return ONLY the complete, updated file content reflecting the instruction —
                no markdown code fences, no explanation, no diff syntax, just the raw new file content.
                """.formatted(project.getPlaywrightVersion(), filePath);

        String userPrompt = "현재 파일 내용:\n```\n"
                + (currentContent != null ? currentContent : "")
                + "\n```\n\n요청 사항: " + instruction;

        String raw = llmGatewayService.chat(project.getAiModelProvider(), systemPrompt, userPrompt);
        return stripMarkdownFence(raw);
    }

    private String stripMarkdownFence(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(typescript|ts|javascript|js)?", "").trim();
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3).trim();
            }
        }
        return trimmed;
    }
}
