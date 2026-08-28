package com.playops.api.controller;

import com.playops.api.dto.AiJobFileDiff;
import com.playops.api.dto.AiJobResponse;
import com.playops.api.dto.CreateAiJobRequest;
import com.playops.api.dto.CreateTemplateGenerateJobRequest;
import com.playops.api.entity.AiJob;
import com.playops.api.entity.User;
import com.playops.api.entity.UserRole;
import com.playops.api.exception.ApiException;
import com.playops.api.service.AiJobService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class AiJobController {

    private final AiJobService aiJobService;

    public AiJobController(AiJobService aiJobService) {
        this.aiJobService = aiJobService;
    }

    @PostMapping("/api/projects/{projectId}/ai-jobs")
    public AiJobResponse create(
            @PathVariable String projectId,
            @RequestBody CreateAiJobRequest body,
            HttpServletRequest request
    ) {
        User user = currentUser(request);
        AiJob job = aiJobService.createCodeFixJob(
                projectId,
                body.getFailedExecutionId(),
                body.getTargetSpecPath(),
                body.getInstruction(),
                user != null ? user.getId() : null
        );
        return AiJobResponse.from(job);
    }

    @PostMapping("/api/projects/{projectId}/ai-jobs/template-generate")
    public AiJobResponse createTemplateGenerate(
            @PathVariable String projectId,
            @RequestBody CreateTemplateGenerateJobRequest body,
            HttpServletRequest request
    ) {
        User user = currentUser(request);
        AiJob job = aiJobService.createTemplateGenerateJob(
                projectId,
                body.getTargetSpecPath(),
                body.getInstruction(),
                user != null ? user.getId() : null
        );
        return AiJobResponse.from(job);
    }

    @GetMapping("/api/projects/{projectId}/ai-jobs")
    public List<AiJobResponse> listByProject(@PathVariable String projectId) {
        return aiJobService.listByProject(projectId).stream().map(AiJobResponse::from).toList();
    }

    @GetMapping("/api/ai-jobs/needs-review")
    public List<AiJobResponse> needsReview(HttpServletRequest request) {
        requireAdmin(request);
        return aiJobService.listNeedsReview().stream().map(AiJobResponse::from).toList();
    }

    @GetMapping("/api/ai-jobs/{id}/diff")
    public List<AiJobFileDiff> diff(@PathVariable Long id, HttpServletRequest request) {
        requireAdmin(request);
        return aiJobService.readChangedFileContents(id);
    }

    @PostMapping("/api/ai-jobs/{id}/approve")
    public AiJobResponse approve(@PathVariable Long id, HttpServletRequest request) {
        User user = requireAdmin(request);
        return AiJobResponse.from(aiJobService.approve(id, user.getId()));
    }

    @PostMapping("/api/ai-jobs/{id}/reject")
    public AiJobResponse reject(@PathVariable Long id, HttpServletRequest request) {
        User user = requireAdmin(request);
        return AiJobResponse.from(aiJobService.reject(id, user.getId()));
    }

    private User currentUser(HttpServletRequest request) {
        return (User) request.getAttribute("currentUser");
    }

    private User requireAdmin(HttpServletRequest request) {
        User user = currentUser(request);
        if (user == null || user.getRole() != UserRole.ADMIN) {
            throw new ApiException(403, "Admin access required");
        }
        return user;
    }
}
