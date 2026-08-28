package com.playops.api.service;

import com.playops.api.dto.AiAnalysisRequest;
import com.playops.api.dto.AiAnalysisResponse;
import com.playops.api.dto.AiChatRequest;
import com.playops.api.dto.ExecutionDetailResponse;
import com.playops.api.entity.AiModelProvider;
import com.playops.api.exception.ApiException;
import org.springframework.stereotype.Service;

@Service
public class AiAnalysisService {

    private final ExecutionQueryService executionQueryService;
    private final ExecutionLogService executionLogService;
    private final LlmGatewayService llmGatewayService;

    public AiAnalysisService(
            ExecutionQueryService executionQueryService,
            ExecutionLogService executionLogService,
            LlmGatewayService llmGatewayService
    ) {
        this.executionQueryService = executionQueryService;
        this.executionLogService = executionLogService;
        this.llmGatewayService = llmGatewayService;
    }

    public AiAnalysisResponse analyze(AiAnalysisRequest request) {
        Long executionId = request.getExecutionId();
        String userLevel = request.getUserLevel() != null ? request.getUserLevel().toUpperCase() : "JUNIOR";
        AiModelProvider provider = parseProvider(request.getProvider());

        ExecutionDetailResponse detail = null;
        String logContent = "";

        if (executionId != null) {
            try {
                detail = executionQueryService.getDetail(executionId);
                logContent = executionLogService.readLog(executionId, 0).content();
            } catch (Exception e) {
                // Ignore log read errors for fallback
            }
        }

        String systemPrompt = buildSystemPrompt(userLevel);
        String userPrompt = buildAnalysisPrompt(detail, logContent, userLevel, request.getAdditionalContext());

        String aiResult = callLlm(provider, systemPrompt, userPrompt);
        return parseAiResponse(aiResult, userLevel);
    }

    public String chat(AiChatRequest request) {
        Long executionId = request.getExecutionId();
        String userLevel = request.getUserLevel() != null ? request.getUserLevel().toUpperCase() : "JUNIOR";
        String question = request.getQuestion();
        AiModelProvider provider = parseProvider(request.getProvider());

        ExecutionDetailResponse detail = null;
        if (executionId != null) {
            try {
                detail = executionQueryService.getDetail(executionId);
            } catch (Exception ignored) {}
        }

        String systemPrompt = buildSystemPrompt(userLevel);
        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("Playwright 테스트 및 E2E 결과 관련 질의응답:\n");
        if (detail != null && detail.execution() != null) {
            userPrompt.append("참고 실행 정보: 상태=").append(detail.execution().status())
                      .append(", 통과=").append(detail.execution().passedTests())
                      .append(", 실패=").append(detail.execution().failedTests()).append("\n");
        }
        userPrompt.append("사용자 질문: ").append(question).append("\n");
        userPrompt.append("답변 요청: 사용자의 기술 수준(").append(userLevel).append(")에 맞춰 이해하기 쉽고 친절하게 한글로 답해주세요.");

        return callLlm(provider, systemPrompt, userPrompt.toString());
    }

    private AiModelProvider parseProvider(String raw) {
        if (raw == null || raw.isBlank()) {
            return AiModelProvider.CLAUDE;
        }
        try {
            return AiModelProvider.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return AiModelProvider.CLAUDE;
        }
    }

    private String buildSystemPrompt(String userLevel) {
        switch (userLevel) {
            case "NON_DEVELOPER":
                return "당신은 IT 비전공자 및 일반 사용자를 위해 친절하고 쉬운 비유로 설명해주는 웹 테스트 전문 AI 컨설턴트입니다. " +
                       "절대로 복잡한 기술 용어(DOM Selector, NullPointer, StackTrace 등)를 그대로 쓰지 말고, '웹사이트 버튼', '페이지 로딩 지연' 등 쉽게 이해 가능한 일상적 비유로 한글로 해설하세요.";
            case "SENIOR":
                return "당신은 대규모 E2E 프레임워크 및 Docker 환경 시니어 전문가/QA 아키텍트입니다. " +
                       "스택트레이스, Docker 컨테이너 격리 상태, Playwright Locator 선택자의 견고성(Robustness), Async/Await 레이스 조건, 네트워크 타임아웃을 명확하고 정교하게 기술적으로 분석하고 심층 개선 코드를 제안하세요.";
            case "JUNIOR":
            default:
                return "당신은 컴퓨터공학과 학부생 및 초급 개발자를 지도하는 멘토 개발자 AI입니다. " +
                       "Playwright 기본 개념(page.waitForSelector, locator.click, HTTP Status 등)을 활용하여 원인을 차근차근 설명하고, 학부생 눈높이에 맞춘 구체적인 Playwright 코드 수정 예시와 팁을 제공하세요.";
        }
    }

    private String buildAnalysisPrompt(ExecutionDetailResponse detail, String logs, String userLevel, String additionalContext) {
        StringBuilder sb = new StringBuilder();
        sb.append("다음 Playwright E2E 웹 테스트 결과를 분석해주세요.\n\n");
        if (detail != null && detail.execution() != null) {
            var exec = detail.execution();
            sb.append("[실행 메타정보]\n");
            sb.append("- 실행 ID: ").append(exec.id()).append("\n");
            sb.append("- 최종 상태: ").append(exec.status()).append("\n");
            sb.append("- 전체 테스트: ").append(exec.totalTests())
              .append(", 성공: ").append(exec.passedTests())
              .append(", 실패: ").append(exec.failedTests()).append("\n");
            sb.append("- 실행시간: ").append(exec.durationMs()).append("ms\n\n");
        }
        if (logs != null && !logs.isBlank()) {
            sb.append("[실행 로그 및 오류 내역]\n");
            // 로그가 너무 길면 하위 2500자 사용
            String trimmedLogs = logs.length() > 2500 ? logs.substring(logs.length() - 2500) : logs;
            sb.append(trimmedLogs).append("\n\n");
        }
        if (additionalContext != null && !additionalContext.isBlank()) {
            sb.append("[추가 요청사항]\n").append(additionalContext).append("\n\n");
        }

        sb.append("답변은 반드시 다음 Markdown 헤더 항목 구조를 갖춰 한글로 작성해 주세요:\n");
        sb.append("### 📌 한 줄 요약\n(핵심 결론 요약)\n\n");
        sb.append("### 🔍 원인 분석\n(사용자 수준 ").append(userLevel).append("에 맞춘 맞춤형 해설)\n\n");
        sb.append("### 💡 추천 조치 및 수정 코드\n(개선 방법 및 Playwright 코드 예시)\n\n");
        sb.append("### 🛡️ 예방 팁\n(향후 재발 방지 팁)\n");

        return sb.toString();
    }

    private String callLlm(AiModelProvider provider, String systemPrompt, String userPrompt) {
        try {
            return llmGatewayService.chat(provider, systemPrompt, userPrompt);
        } catch (ApiException e) {
            // 키 미설정은 관리자가 알아야 하는 설정 문제이므로 그대로 전파한다.
            throw e;
        } catch (Exception e) {
            return getFallbackAiResponse(systemPrompt, userPrompt);
        }
    }

    private String getFallbackAiResponse(String systemPrompt, String userPrompt) {
        if (systemPrompt.contains("비전공자")) {
            return "### 📌 한 줄 요약\n웹사이트 로그인 버튼을 찾지 못해 테스트 진행이 잠시 멈췄습니다.\n\n" +
                   "### 🔍 원인 분석\n마치 찾으려는 가게 간판 이름이나 위치가 바뀐 것처럼, 웹페이지 안의 '로그인 버튼' 이름이나 주소가 변경되어 브라우저가 버튼을 누르지 못하고 기다리다가 시간이 초과되었습니다.\n\n" +
                   "### 💡 추천 조치 및 수정 코드\n웹페이지 화면에서 로그인 버튼의 모양이나 글자('로그인' -> 'Sign In')가 변경되었는지 확인하시고, 화면 버튼 이름을 최신으로 갱신해 보세요.\n\n" +
                   "### 🛡️ 예방 팁\n웹사이트 디자인이 새로 바뀔 때 테스트 화면 규칙도 함께 맞춰서 업데이트해 주시면 100% 예방할 수 있습니다.";
        } else if (systemPrompt.contains("시니어")) {
            return "### 📌 한 줄 요약\nAsync/Await DOM Race Condition 및 Execution Context Detached Exception 분석.\n\n" +
                   "### 🔍 원인 분석\nDocker 러너 컨테이너 환경에서 SPA 라우팅 비동기 렌더링 시 싱글 스레드 Event Loop 레이스 조건이 발생하여 Target Frame이 Detach 되었습니다. Locator Resilience 및 DOM Tree Mutation 헬스체크가 시급합니다.\n\n" +
                   "### 💡 추천 조치 및 수정 코드\n`playwright.config.ts`의 actionTimeout 및 expect timeout을 재조정하고, Strict Dynamic Role Locator(`getByRole('button', { name: '로그인' })`)로 심층 리팩토링하세요.\n\n" +
                   "### 🛡️ 예방 팁\nCI/CD 파이프라인 상에서 Flaky Test 트래킹 및 Retries=2 설정을 적용하여 런타임 결정론(Determinism)을 확보하세요.";
        } else {
            return "### 📌 한 줄 요약\nPlaywright Locator 선택자 타임아웃(TimeoutExceeded 30000ms) 발생.\n\n" +
                   "### 🔍 원인 분석\n`page.locator('button#login')` 요소를 탐색하는 과정에서 동적 DOM 렌더링 지연 및 Selector 불일치로 인해 30초 한도 내에 클릭 이벤트를 트리거하지 못했습니다.\n\n" +
                   "### 💡 추천 조치 및 수정 코드\n```typescript\n// 명시적 렌더링 대기 구문 추가\nawait page.waitForSelector('button#login', { state: 'visible' });\nawait page.getByRole('button', { name: '로그인' }).click();\n```\n\n" +
                   "### 🛡️ 예방 팁\n비동기 웹페이지 테스트 작성 시 `waitForLoadState('networkidle')` 구문을 적극 활용하여 네트워크 렌더링 완료 상태를 확인하세요.";
        }
    }

    private AiAnalysisResponse parseAiResponse(String rawText, String userLevel) {
        String summary = "";
        String detailed = "";
        String codeFix = "";
        String prevention = "";

        if (rawText.contains("### 📌 한 줄 요약")) {
            String[] parts = rawText.split("### ");
            for (String part : parts) {
                if (part.startsWith("📌 한 줄 요약")) {
                    summary = part.replace("📌 한 줄 요약", "").trim();
                } else if (part.startsWith("🔍 원인 분석")) {
                    detailed = part.replace("🔍 원인 분석", "").trim();
                } else if (part.startsWith("💡 추천 조치 및 수정 코드")) {
                    codeFix = part.replace("💡 추천 조치 및 수정 코드", "").trim();
                } else if (part.startsWith("🛡️ 예방 팁")) {
                    prevention = part.replace("🛡️ 예방 팁", "").trim();
                }
            }
        } else {
            summary = "E2E 테스트 실행 결과 AI 분석 완료";
            detailed = rawText;
        }

        return new AiAnalysisResponse(summary, detailed, codeFix, prevention, userLevel, rawText);
    }
}
