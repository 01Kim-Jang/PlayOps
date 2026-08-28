package com.playops.api.controller;

import com.playops.api.dto.ExecutionDetailResponse;
import com.playops.api.dto.ExecutionLogResponse;
import com.playops.api.exception.ApiException;
import com.playops.api.service.ExecutionLogService;
import com.playops.api.service.ExecutionQueryService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.file.Paths;

@RestController
@RequestMapping("/api/executions")
public class ExecutionController {

    private final ExecutionQueryService executionQueryService;
    private final ExecutionLogService executionLogService;

    public ExecutionController(
            ExecutionQueryService executionQueryService,
            ExecutionLogService executionLogService
    ) {
        this.executionQueryService = executionQueryService;
        this.executionLogService = executionLogService;
    }

    @GetMapping("/{executionId}")
    public ExecutionDetailResponse detail(@PathVariable Long executionId) {
        return executionQueryService.getDetail(executionId);
    }

    @PostMapping("/{executionId}/cancel")
    public com.playops.api.dto.ExecutionResponse cancel(@PathVariable Long executionId) {
        return executionQueryService.cancelExecution(executionId);
    }

    @DeleteMapping("/{executionId}")
    public ResponseEntity<Void> delete(@PathVariable Long executionId) {
        executionQueryService.deleteExecution(executionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{executionId}/logs")
    public ExecutionLogResponse logs(
            @PathVariable Long executionId,
            @RequestParam(defaultValue = "0") long offset
    ) {
        return executionLogService.readLog(executionId, offset);
    }

    @GetMapping(value = "/{executionId}/logs/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamLogs(@PathVariable Long executionId) {
        return executionLogService.streamLogs(executionId);
    }

    @GetMapping("/{executionId}/artifact")
    public ResponseEntity<Resource> artifact(
            @PathVariable Long executionId,
            @RequestParam String path
    ) {
        if (path == null || path.isBlank()) {
            throw new ApiException(400, "path is required");
        }
        Resource resource = executionQueryService.getArtifactResource(executionId, path);
        MediaType mediaType = executionQueryService.resolveMediaType(path);
        String filename = Paths.get(path).getFileName().toString();
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(resource);
    }
}
