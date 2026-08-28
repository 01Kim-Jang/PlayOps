package com.playops.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.playops.api.entity.AiJob;
import com.playops.api.entity.Execution;
import com.playops.api.entity.ExecutionStatus;
import com.playops.api.entity.PostApplyVerificationStatus;
import com.playops.api.entity.Project;
import com.playops.api.repository.AiJobRepository;
import com.playops.api.repository.ExecutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * docs/ai-job-spec.md의 "사후 안전망: 자동 Revert"를 구현한다.
 *
 * git 연동된 프로젝트(AiJob.appliedCommitSha가 있음)는 GitCommitService로 실제 git revert + push를 수행한다.
 * git 미연동 프로젝트는 커밋 자체가 없으므로, AiJobService.applyDiff가 파일마다 남겨둔 적용 직전 스냅샷으로
 * 직접 복원하는 파일 레벨 revert로 대체한다 — 사용자가 보는 결과(회귀가 감지되면 AI가 건드리기 전 상태로
 * 돌아간다)는 두 경우 모두 동일하다.
 */
@Service
public class AiPostApplyVerificationService {

    private static final Logger log = LoggerFactory.getLogger(AiPostApplyVerificationService.class);

    private final ExecutionRepository executionRepository;
    private final TestExecutionService testExecutionService;
    private final AiJobRepository aiJobRepository;
    private final ProjectService projectService;
    private final DockerRunnerService dockerRunnerService;
    private final AiRunnerDockerService aiRunnerDockerService;
    private final SlackNotificationService slackNotificationService;
    private final GitCommitService gitCommitService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiPostApplyVerificationService(
            ExecutionRepository executionRepository,
            TestExecutionService testExecutionService,
            AiJobRepository aiJobRepository,
            ProjectService projectService,
            DockerRunnerService dockerRunnerService,
            AiRunnerDockerService aiRunnerDockerService,
            SlackNotificationService slackNotificationService,
            GitCommitService gitCommitService
    ) {
        this.executionRepository = executionRepository;
        this.testExecutionService = testExecutionService;
        this.aiJobRepository = aiJobRepository;
        this.projectService = projectService;
        this.dockerRunnerService = dockerRunnerService;
        this.aiRunnerDockerService = aiRunnerDockerService;
        this.slackNotificationService = slackNotificationService;
        this.gitCommitService = gitCommitService;
    }

    /** APPLIED 직후(자동이든 사람 승인이든) 호출한다. 다음 스케줄을 기다리지 않고 즉시 전체 프로젝트 실행을 하나 더 만든다. */
    public void triggerVerification(AiJob job, Project project) {
        Execution execution = testExecutionService.createExecution(project.getProjectId(), null, null, null, null);
        execution.setSource("AI_POST_APPLY");
        executionRepository.save(execution);

        job.setPostApplyVerificationExecutionId(execution.getId());
        job.setPostApplyVerificationStatus(PostApplyVerificationStatus.PENDING);
        aiJobRepository.save(job);

        dockerRunnerService.logOperation(project.getProjectId(),
                "[playops] AI 적용(job#" + job.getId() + ") 사후 검증 실행 시작: execution#" + execution.getId());

        testExecutionService.runExecutionAsync(execution.getId());
        pollAndFinalize(job.getId(), execution.getId(), project.getTimeout());
    }

    @Async("aiVerificationExecutor")
    public void pollAndFinalize(Long jobId, Long executionId, Integer projectTimeoutSeconds) {
        int budgetSeconds = (projectTimeoutSeconds != null ? projectTimeoutSeconds : 300) + 180;
        long deadline = System.currentTimeMillis() + budgetSeconds * 1000L;

        Execution execution = null;
        while (System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            execution = executionRepository.findById(executionId).orElse(null);
            if (execution == null || isTerminal(execution.getStatus())) {
                break;
            }
        }

        AiJob job = aiJobRepository.findById(jobId).orElse(null);
        if (job == null) {
            return;
        }
        if (execution == null || !isTerminal(execution.getStatus())) {
            markSkipped(job, "사후 검증 실행이 시간 내에 끝나지 않았습니다.");
            return;
        }

        finalizeVerification(job, execution);
    }

    private boolean isTerminal(ExecutionStatus status) {
        return status == ExecutionStatus.PASSED || status == ExecutionStatus.FAILED
                || status == ExecutionStatus.ERROR || status == ExecutionStatus.CANCELLED;
    }

    private void finalizeVerification(AiJob job, Execution verificationExecution) {
        Project project = projectService.getProject(job.getProjectId());

        if (verificationExecution.getStatus() == ExecutionStatus.ERROR
                || verificationExecution.getStatus() == ExecutionStatus.CANCELLED) {
            markSkipped(job, "사후 검증 실행이 " + verificationExecution.getStatus() + "로 종료되어 비교할 수 없습니다.");
            return;
        }

        Map<String, Boolean> currentResults = parseResults(verificationExecution);
        if (currentResults.isEmpty()) {
            markSkipped(job, "사후 검증 결과 파일을 읽을 수 없습니다.");
            return;
        }

        Execution baseline = findBaseline(project.getProjectId(), verificationExecution.getId());
        if (baseline == null) {
            markSkipped(job, "비교할 이전 성공 실행(baseline)이 없습니다.");
            return;
        }

        Map<String, Boolean> baselineResults = parseResults(baseline);
        Set<String> excludedFiles = new LinkedHashSet<>();
        for (String file : parseChangedFiles(job)) {
            excludedFiles.add(normalizeFile(file));
        }

        Set<String> regressed = findRegressedCases(baselineResults, currentResults, excludedFiles);
        if (regressed.isEmpty()) {
            markPassed(job, "회귀 없음 (baseline: execution#" + baseline.getId() + ")");
            return;
        }

        log.info("AI job {} 사후 검증에서 회귀 후보 {}건 발견, 재확인 실행", job.getId(), regressed.size());
        Set<String> stillFailing = retryAndCheck(project, regressed, currentResults);

        if (stillFailing.isEmpty()) {
            markPassed(job, "회귀 후보 " + regressed.size() + "건 모두 재실행 시 통과 → 플레이키로 판단, 되돌리지 않음");
            return;
        }

        revert(job, project, stillFailing);
    }

    private Set<String> retryAndCheck(Project project, Set<String> regressedCaseKeys, Map<String, Boolean> firstRunResults) {
        Set<String> files = new LinkedHashSet<>();
        for (String key : regressedCaseKeys) {
            files.add(fileOf(key));
        }
        Execution retryExecution = testExecutionService.createExecution(
                project.getProjectId(), null, null, new ArrayList<>(files), null);
        retryExecution.setSource("AI_POST_APPLY");
        executionRepository.save(retryExecution);

        dockerRunnerService.logOperation(project.getProjectId(),
                "[playops] 사후 검증 회귀 재확인 실행: execution#" + retryExecution.getId() + " (" + files + ")");

        testExecutionService.runExecutionAsync(retryExecution.getId());

        int budgetSeconds = (project.getTimeout() != null ? project.getTimeout() : 300) + 180;
        long deadline = System.currentTimeMillis() + budgetSeconds * 1000L;
        Execution finished = null;
        while (System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            Execution current = executionRepository.findById(retryExecution.getId()).orElse(null);
            if (current == null || isTerminal(current.getStatus())) {
                finished = current;
                break;
            }
        }

        if (finished == null || finished.getStatus() != ExecutionStatus.PASSED && finished.getStatus() != ExecutionStatus.FAILED) {
            // 재실행 자체가 비정상 종료되면 판단할 근거가 없다 — 감수할 오탐/누락 방향과 동일하게 "여전히 실패"로 보수적으로 처리한다.
            return regressedCaseKeys;
        }

        Map<String, Boolean> retryResults = parseResults(finished);
        Set<String> stillFailing = new LinkedHashSet<>();
        for (String key : regressedCaseKeys) {
            Boolean passed = retryResults.get(key);
            if (passed == null || !passed) {
                stillFailing.add(key);
            }
        }
        return stillFailing;
    }

    private void revert(AiJob job, Project project, Set<String> stillFailingCases) {
        String note;
        if (job.getAppliedCommitSha() != null && !job.getAppliedCommitSha().isBlank()) {
            try {
                String revertSha = gitCommitService.revertCommit(project, job.getAppliedCommitSha());
                job.setRevertCommit(revertSha);
                note = "회귀 확정(" + stillFailingCases.size() + "건) → git revert 완료 (commit " + revertSha + ")";
            } catch (Exception e) {
                log.error("AI job {} git revert 실패, 파일 스냅샷 복원으로 대체", job.getId(), e);
                boolean restored = restoreFromSnapshots(job, project);
                note = "회귀 확정(" + stillFailingCases.size() + "건) → git revert 실패(" + e.getMessage() + "), "
                        + (restored ? "파일 스냅샷으로 대신 복원함" : "스냅샷도 없어 복원하지 못함, 수동 확인 필요");
            }
        } else {
            boolean restored = restoreFromSnapshots(job, project);
            note = "회귀 확정(" + stillFailingCases.size() + "건) → "
                    + (restored ? "적용 전 내용으로 자동 복원 완료" : "적용 전 스냅샷이 없어 자동 복원하지 못함, 수동 확인 필요");
        }

        job.setPostApplyVerificationStatus(PostApplyVerificationStatus.REGRESSED);
        job.setRevertedAt(Instant.now());
        job.setSummary(appendNote(job.getSummary(), note));
        aiJobRepository.save(job);

        dockerRunnerService.logOperation(project.getProjectId(), "[playops] " + note);
        log.warn("AI job {} regressed and reverted: {}", job.getId(), note);

        slackNotificationService.send(
                ":rotating_light: [" + project.getProjectName() + "] AI 적용 후 회귀 감지 → 자동 되돌림 — job #" + job.getId()
                        + "\n대상: " + job.getTargetSpecPath()
                        + "\n회귀 케이스: " + stillFailingCases.size() + "건"
                        + "\n" + note
        );
    }

    /** git 미연동(또는 git revert 실패) 프로젝트용 폴백 — applyDiff가 남긴 파일별 스냅샷으로 하나씩 복원한다. */
    private boolean restoreFromSnapshots(AiJob job, Project project) {
        Path snapshotDir = aiRunnerDockerService.aiJobDir(job.getId()).resolve("pre-apply-snapshot");
        boolean any = false;
        for (String file : parseChangedFiles(job)) {
            Path snapshot = snapshotDir.resolve(safeFileName(file));
            if (!Files.exists(snapshot)) {
                continue;
            }
            try {
                Path targetFile = projectService.getProjectPath(project.getProjectId()).resolve(file);
                Files.copy(snapshot, targetFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                any = true;
            } catch (Exception e) {
                log.error("AI job {} 파일 스냅샷 복원 실패 ({})", job.getId(), file, e);
            }
        }
        return any;
    }

    private void markPassed(AiJob job, String note) {
        job.setPostApplyVerificationStatus(PostApplyVerificationStatus.PASSED);
        job.setSummary(appendNote(job.getSummary(), "사후 검증 통과: " + note));
        aiJobRepository.save(job);
    }

    private void markSkipped(AiJob job, String reason) {
        job.setPostApplyVerificationStatus(PostApplyVerificationStatus.SKIPPED);
        job.setSummary(appendNote(job.getSummary(), "사후 검증 건너뜀: " + reason));
        aiJobRepository.save(job);
    }

    private String appendNote(String summary, String note) {
        if (summary == null || summary.isBlank()) {
            return note;
        }
        return summary + "\n" + note;
    }

    private Execution findBaseline(String projectId, Long excludeExecutionId) {
        return executionRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .filter(e -> !e.getId().equals(excludeExecutionId))
                .filter(e -> e.getStatus() == ExecutionStatus.PASSED)
                .filter(e -> isBlank(e.getSpecPath()) && isBlank(e.getGrepFilter()))
                .findFirst()
                .orElse(null);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private Set<String> findRegressedCases(Map<String, Boolean> baseline, Map<String, Boolean> current, Set<String> excludedFiles) {
        Set<String> regressed = new LinkedHashSet<>();
        for (Map.Entry<String, Boolean> entry : baseline.entrySet()) {
            String key = entry.getKey();
            if (!Boolean.TRUE.equals(entry.getValue())) {
                continue;
            }
            if (excludedFiles.contains(fileOf(key))) {
                continue;
            }
            Boolean nowPassed = current.get(key);
            if (nowPassed == null || !nowPassed) {
                regressed.add(key);
            }
        }
        return regressed;
    }

    /** AiJobService.parseChangedFiles와 동일 로직. 순환 의존을 피하려고(AiJobService <-> 이 서비스) 여기서도 작게 복제한다. */
    private List<String> parseChangedFiles(AiJob job) {
        List<String> files = new ArrayList<>();
        if (job.getChangedFiles() != null && !job.getChangedFiles().isBlank()) {
            try {
                objectMapper.readTree(job.getChangedFiles()).forEach(node -> files.add(node.asText()));
            } catch (Exception ignored) {}
        }
        if (files.isEmpty() && job.getTargetSpecPath() != null) {
            files.add(job.getTargetSpecPath());
        }
        return files;
    }

    private String safeFileName(String relativePath) {
        return relativePath.replace("/", "__").replace("\\", "__");
    }

    private String fileOf(String caseKey) {
        int idx = caseKey.indexOf(" :: ");
        return idx >= 0 ? caseKey.substring(0, idx) : caseKey;
    }

    private String normalizeFile(String path) {
        return path == null ? null : path.replace('\\', '/');
    }

    /** Playwright JSON reporter(results.json) 결과를 (파일::제목경로) -> 통과여부 맵으로 펼친다. */
    private Map<String, Boolean> parseResults(Execution execution) {
        Map<String, Boolean> out = new HashMap<>();
        if (execution.getReportPath() == null) {
            return out;
        }
        Path resultsFile = Path.of(execution.getReportPath(), "results.json");
        if (!Files.exists(resultsFile)) {
            return out;
        }
        try {
            JsonNode root = objectMapper.readTree(resultsFile.toFile());
            for (JsonNode suite : root.path("suites")) {
                walkSuite(suite, null, new ArrayList<>(), out);
            }
        } catch (Exception e) {
            log.warn("results.json 파싱 실패 (execution#{}): {}", execution.getId(), e.getMessage());
        }
        return out;
    }

    private void walkSuite(JsonNode suite, String inheritedFile, List<String> parentTitles, Map<String, Boolean> out) {
        String file = suite.hasNonNull("file") && !suite.path("file").asText().isBlank()
                ? normalizeFile(suite.path("file").asText())
                : inheritedFile;

        List<String> titles = new ArrayList<>(parentTitles);
        String suiteTitle = suite.path("title").asText(null);
        if (suiteTitle != null && !suiteTitle.isBlank()) {
            titles.add(suiteTitle);
        }

        for (JsonNode spec : suite.path("specs")) {
            List<String> specTitles = new ArrayList<>(titles);
            specTitles.add(spec.path("title").asText(""));
            String key = (file != null ? file : "?") + " :: " + String.join(" > ", specTitles);
            boolean ok = spec.path("ok").asBoolean(true);
            out.put(key, ok);
        }

        for (JsonNode child : suite.path("suites")) {
            walkSuite(child, file, titles, out);
        }
    }
}
