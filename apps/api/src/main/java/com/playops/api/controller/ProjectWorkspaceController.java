package com.playops.api.controller;

import com.playops.api.dto.*;
import com.playops.api.service.ExecutionQueryService;
import com.playops.api.service.FileStorageService;
import com.playops.api.service.PlaywrightScenarioService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}")
public class ProjectWorkspaceController {

    private final FileStorageService fileStorageService;
    private final PlaywrightScenarioService scenarioService;
    private final ExecutionQueryService executionQueryService;

    public ProjectWorkspaceController(
            FileStorageService fileStorageService,
            PlaywrightScenarioService scenarioService,
            ExecutionQueryService executionQueryService
    ) {
        this.fileStorageService = fileStorageService;
        this.scenarioService = scenarioService;
        this.executionQueryService = executionQueryService;
    }

    @GetMapping("/files/content")
    public ProjectFileContent readFile(
            @PathVariable String projectId,
            @RequestParam String path
    ) {
        return new ProjectFileContent(path, fileStorageService.readFile(projectId, path));
    }

    @PutMapping("/files/content")
    public ProjectFileContent saveFile(
            @PathVariable String projectId,
            @RequestParam String path,
            @RequestBody SaveFileRequest request
    ) {
        fileStorageService.writeFile(projectId, path, request.content());
        return new ProjectFileContent(path, fileStorageService.readFile(projectId, path));
    }

    @GetMapping("/scenarios")
    public ScenarioTreeResponse scenarios(@PathVariable String projectId) {
        return scenarioService.analyze(projectId);
    }

    @PutMapping("/scenarios/usage")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateScenarioUsage(
            @PathVariable String projectId,
            @RequestBody ScenarioUsageRequest request
    ) {
        scenarioService.updateUsage(
                projectId,
                request.nodeType(),
                request.specPath(),
                request.grep(),
                request.enabled()
        );
    }

    @GetMapping("/executions")
    public List<ExecutionResponse> executions(@PathVariable String projectId) {
        return executionQueryService.listByProject(projectId);
    }

    @DeleteMapping("/executions")
    public void resetExecutions(@PathVariable String projectId) {
        executionQueryService.resetProjectExecutions(projectId);
    }

    @PostMapping("/executions")
    public ExecutionResponse runTests(
            @PathVariable String projectId,
            @RequestBody(required = false) RunTestRequest request
    ) {
        String grep = request != null ? request.grep() : null;
        String specPath = request != null ? request.specPath() : null;
        List<String> specPaths = request != null ? request.specPaths() : null;
        String caseTitle = request != null ? request.caseTitle() : null;
        boolean sequential = request != null && Boolean.TRUE.equals(request.sequential());
        return executionQueryService.triggerRun(projectId, grep, specPath, specPaths, caseTitle, sequential);
    }

    @GetMapping("/executions/history")
    public List<ExecutionResponse> executionHistory(
            @PathVariable String projectId,
            @RequestParam String grep
    ) {
        return executionQueryService.historyByGrep(projectId, grep);
    }
}
