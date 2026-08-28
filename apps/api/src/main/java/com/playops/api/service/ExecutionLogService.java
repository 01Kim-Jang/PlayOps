package com.playops.api.service;

import com.playops.api.config.PlayOpsProperties;
import com.playops.api.entity.Execution;
import com.playops.api.entity.ExecutionStatus;
import com.playops.api.exception.ApiException;
import com.playops.api.repository.ExecutionRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class ExecutionLogService {

    private static final java.util.regex.Pattern ANSI_CONTROL = java.util.regex.Pattern.compile(
            "\\u001B(?:\\[[0-?]*[ -/]*[@-~]|\\][^\\u0007]*(?:\\u0007|\\u001B\\\\)|[PX^_].*?\\u001B\\\\|[@-Z\\\\-_])"
    );

    private final ExecutionRepository executionRepository;
    private final PlayOpsProperties properties;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    public ExecutionLogService(ExecutionRepository executionRepository, PlayOpsProperties properties) {
        this.executionRepository = executionRepository;
        this.properties = properties;
    }

    public Path resolveReportDir(Execution execution) {
        String reportPath = execution.getReportPath();
        if (reportPath == null) {
            return Path.of(properties.storageRoot(), "reports", execution.getProjectId(), String.valueOf(execution.getId()));
        }
        Path dir = Path.of(reportPath);
        if (dir.isAbsolute()) {
            return dir.normalize();
        }
        if (reportPath.startsWith("reports/")) {
            return Path.of(properties.storageRoot()).resolve(reportPath).normalize();
        }
        return Path.of(properties.storageRoot(), "reports", execution.getProjectId(), String.valueOf(execution.getId())).normalize();
    }

    public void initStreamLog(Path reportDir) throws IOException {
        Files.createDirectories(reportDir);
        Files.writeString(reportDir.resolve("stream.log"), "", StandardCharsets.UTF_8);
    }

    public synchronized void append(Path reportDir, String line) {
        try {
            Path streamLog = reportDir.resolve("stream.log");
            if (!Files.exists(streamLog)) {
                Files.createDirectories(reportDir);
                Files.writeString(streamLog, "", StandardCharsets.UTF_8);
            }
            Files.writeString(streamLog, sanitize(line) + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {}
    }

    private String sanitize(String line) {
        return ANSI_CONTROL.matcher(line).replaceAll("");
    }

    public com.playops.api.dto.ExecutionLogResponse readLog(Long executionId, long offset) {
        Execution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new ApiException(404, "Execution not found"));

        Path streamLog = resolveReportDir(execution).resolve("stream.log");
        String content = "";
        long newOffset = offset;

        if (Files.exists(streamLog)) {
            try {
                String full = Files.readString(streamLog, StandardCharsets.UTF_8);
                if (offset >= full.length()) {
                    content = "";
                    newOffset = full.length();
                } else {
                    content = full.substring((int) offset);
                    newOffset = full.length();
                }
            } catch (IOException e) {
                throw new ApiException(500, "Failed to read log: " + e.getMessage());
            }
        }

        boolean finished = isTerminal(execution.getStatus());
        return new com.playops.api.dto.ExecutionLogResponse(content, newOffset, finished, execution.getStatus().name());
    }

    public SseEmitter streamLogs(Long executionId) {
        SseEmitter emitter = new SseEmitter(600_000L);
        final long[] offset = {0};

        Runnable poll = () -> {
            try {
                var chunk = readLog(executionId, offset[0]);
                if (!chunk.content().isEmpty()) {
                    emitter.send(SseEmitter.event().name("log").data(chunk.content()));
                    offset[0] = chunk.offset();
                }
                if (chunk.finished()) {
                    emitter.send(SseEmitter.event().name("done").data(chunk.status()));
                    emitter.complete();
                }
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        };

        var task = scheduler.scheduleAtFixedRate(poll, 0, 500, TimeUnit.MILLISECONDS);
        emitter.onCompletion(() -> task.cancel(false));
        emitter.onTimeout(() -> task.cancel(false));
        emitter.onError(e -> task.cancel(false));

        return emitter;
    }

    private boolean isTerminal(ExecutionStatus status) {
        return status == ExecutionStatus.PASSED
                || status == ExecutionStatus.FAILED
                || status == ExecutionStatus.ERROR
                || status == ExecutionStatus.CANCELLED;
    }
}
