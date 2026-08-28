package com.playops.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playops.api.config.PlayOpsProperties;
import com.playops.api.dto.RunnerCapacityRequest;
import com.playops.api.exception.ApiException;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class RunnerCapacitySettingsService {

    private static final RunnerCapacityRequest DEFAULT_SETTINGS = new RunnerCapacityRequest(
            true,
            2,
            4,
            20,
            60
    );

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Path settingsPath;
    private RunnerCapacityRequest settings;

    public RunnerCapacitySettingsService(PlayOpsProperties properties) {
        this.settingsPath = Path.of(properties.storageRoot(), "config", "runner-capacity.json")
                .toAbsolutePath()
                .normalize();
        this.settings = load();
    }

    public synchronized RunnerCapacityRequest currentSettings() {
        return settings;
    }

    public synchronized RunnerCapacityRequest update(RunnerCapacityRequest request) {
        settings = normalize(request);
        save(settings);
        return settings;
    }

    private RunnerCapacityRequest load() {
        if (!Files.exists(settingsPath)) {
            return DEFAULT_SETTINGS;
        }
        try {
            return normalize(objectMapper.readValue(settingsPath.toFile(), RunnerCapacityRequest.class));
        } catch (Exception e) {
            return DEFAULT_SETTINGS;
        }
    }

    private void save(RunnerCapacityRequest settings) {
        try {
            Files.createDirectories(settingsPath.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(settingsPath.toFile(), settings);
        } catch (Exception e) {
            throw new ApiException(500, "Failed to save runner capacity settings: " + e.getMessage());
        }
    }

    public RunnerCapacityRequest normalize(RunnerCapacityRequest request) {
        boolean autoScaleEnabled = request != null && request.autoScaleEnabled() != null
                ? request.autoScaleEnabled()
                : DEFAULT_SETTINGS.autoScaleEnabled();
        int baseConcurrency = clamp(
                request != null ? request.baseConcurrency() : null,
                DEFAULT_SETTINGS.baseConcurrency(),
                1,
                64
        );
        int maxConcurrency = clamp(
                request != null ? request.maxConcurrency() : null,
                DEFAULT_SETTINGS.maxConcurrency(),
                baseConcurrency,
                128
        );
        int queueCapacity = clamp(
                request != null ? request.queueCapacity() : null,
                DEFAULT_SETTINGS.queueCapacity(),
                0,
                10000
        );
        int scaleDownIdleSeconds = clamp(
                request != null ? request.scaleDownIdleSeconds() : null,
                DEFAULT_SETTINGS.scaleDownIdleSeconds(),
                1,
                3600
        );
        return new RunnerCapacityRequest(
                autoScaleEnabled,
                baseConcurrency,
                maxConcurrency,
                queueCapacity,
                scaleDownIdleSeconds
        );
    }

    public int effectiveMaxConcurrency(RunnerCapacityRequest settings) {
        return Boolean.TRUE.equals(settings.autoScaleEnabled())
                ? Math.max(settings.baseConcurrency(), settings.maxConcurrency())
                : settings.baseConcurrency();
    }

    private int clamp(Integer value, int defaultValue, int min, int max) {
        int resolved = value != null ? value : defaultValue;
        return Math.max(min, Math.min(max, resolved));
    }
}
