package com.playops.api.service;

import com.playops.api.dto.AiBootstrapResponse;
import com.playops.api.dto.AiTemplateRequest;
import com.playops.api.dto.AiTemplateResponse;
import com.playops.api.dto.ExecutionResponse;
import com.playops.api.entity.Execution;
import com.playops.api.entity.Project;
import com.playops.api.exception.ApiException;
import com.playops.api.repository.ExecutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 프로젝트 등록 직후 "AI가 초기 테스트 케이스를 만들고, 그 자리에서 실제로 실행해 통과 여부까지 보여주는" 흐름.
 *
 * 기존 조각들을 그대로 이어붙인 것이다 — 생성은 {@link AiTemplateService}(시나리오 탭에서 쓰던 것),
 * 파일 기록은 {@link FileStorageService}, 실행은 {@link ExecutionQueryService#triggerRun}(수동 실행과 동일 경로).
 * AI가 코드만 뱉고 끝나던 기존 경험과 달리, 실행 결과(통과/실패)까지 한 번에 확인시켜 주는 것이 목적이다.
 */
@Service
public class AiBootstrapService {

    private static final Logger log = LoggerFactory.getLogger(AiBootstrapService.class);

    /** 실행 이력이 아직 없을 때 쓰는 기본 예상치 (AI 생성 + 러너 기동 + 단일 spec 실행 기준). */
    private static final long DEFAULT_ESTIMATED_DURATION_MS = 60_000L;

    private final ProjectService projectService;
    private final AiTemplateService aiTemplateService;
    private final FileStorageService fileStorageService;
    private final ExecutionQueryService executionQueryService;
    private final ExecutionRepository executionRepository;

    public AiBootstrapService(
            ProjectService projectService,
            AiTemplateService aiTemplateService,
            FileStorageService fileStorageService,
            ExecutionQueryService executionQueryService,
            ExecutionRepository executionRepository
    ) {
        this.projectService = projectService;
        this.aiTemplateService = aiTemplateService;
        this.fileStorageService = fileStorageService;
        this.executionQueryService = executionQueryService;
        this.executionRepository = executionRepository;
    }

    public AiBootstrapResponse bootstrap(String projectId, String instruction, String provider) {
        if (instruction == null || instruction.isBlank()) {
            throw new ApiException(400, "어떤 테스트를 만들지 입력하세요.");
        }
        Project project = projectService.getProject(projectId);

        AiTemplateRequest generateRequest = new AiTemplateRequest();
        generateRequest.setUserPrompt(instruction.trim());
        generateRequest.setTargetUrl(project.getBaseUrl());
        generateRequest.setTemplateName(project.getProjectName());
        generateRequest.setProvider(provider);

        AiTemplateResponse generated = aiTemplateService.generateTemplate(generateRequest);

        // 생성 결과를 프로젝트에 기록한다. 등록 직후의 초기 소스이므로 기존 스캐폴드와 같은 성격이라
        // (CODE_FIX처럼) 검토 큐를 거치지 않고 바로 기록해도 되는 지점이다.
        List<String> writtenFiles = new ArrayList<>();
        String specPath = null;
        for (AiTemplateResponse.TemplateFileDto file : generated.getFiles()) {
            String filename = file.getFilename();
            if (filename == null || filename.isBlank()) {
                continue;
            }
            // playwright.config.ts / package.json은 등록 시 스캐폴드가 이미 프로젝트 규약에 맞게 깔아두므로
            // AI가 만든 버전으로 덮어쓰지 않는다 (baseURL·러너 설정이 깨질 수 있다).
            if (!filename.endsWith(".spec.ts") && !filename.endsWith(".spec.js")) {
                continue;
            }
            fileStorageService.writeFile(projectId, filename, file.getContent());
            writtenFiles.add(filename);
            if (specPath == null) {
                specPath = filename;
            }
        }

        if (specPath == null) {
            throw new ApiException(502, "AI가 실행 가능한 spec 파일을 만들지 못했습니다. 요청 내용을 조금 더 구체적으로 적어보세요.");
        }

        ExecutionResponse execution = executionQueryService.triggerRun(
                projectId, null, specPath, null, "AI 초기 케이스: " + instruction.trim(), false);

        log.info("AI bootstrap 실행 시작. project={}, spec={}, execution={}", projectId, specPath, execution.id());

        return new AiBootstrapResponse(execution.id(), writtenFiles, specPath, estimateDurationMs());
    }

    /** 최근 완료된 실행들의 평균 소요시간. 이력이 없으면 기본값을 쓴다. */
    private long estimateDurationMs() {
        List<Execution> recent = executionRepository.findTop20ByDurationMsIsNotNullOrderByCreatedAtDesc();
        if (recent.isEmpty()) {
            return DEFAULT_ESTIMATED_DURATION_MS;
        }
        long sum = 0;
        for (Execution execution : recent) {
            sum += execution.getDurationMs();
        }
        return Math.max(10_000L, sum / recent.size());
    }
}
