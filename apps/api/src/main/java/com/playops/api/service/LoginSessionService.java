package com.playops.api.service;

import com.playops.api.config.PlayOpsProperties;
import com.playops.api.dto.AuthStateStatusResponse;
import com.playops.api.entity.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * 로그인/설정 선행 시나리오(Project.loginSetupSpecPath)가 만든 Playwright storageState(로그인 세션)를 관리한다.
 * 매 테스트 실행마다 UI로 로그인하는 대신, 선행 시나리오를 먼저(필요할 때만) 돌려 세션 파일을 만들고
 * 이후 실행들이 그 세션을 재사용하게 한다 — docs/scenario-env-management.md의 "로그인 세션 관리" 설계를 따른다.
 *
 * 이 기능은 완전히 옵트인이다: Project.loginSetupSpecPath가 비어 있으면 아무 것도 하지 않고,
 * 기존 실행 흐름과 100% 동일하게 동작한다.
 */
@Service
public class LoginSessionService {

    private static final Logger log = LoggerFactory.getLogger(LoginSessionService.class);
    private static final int DEFAULT_MAX_AGE_MINUTES = 720;

    private final PlayOpsProperties properties;

    public LoginSessionService(PlayOpsProperties properties) {
        this.properties = properties;
    }

    public boolean isConfigured(Project project) {
        return project.getLoginSetupSpecPath() != null && !project.getLoginSetupSpecPath().isBlank();
    }

    /** storageState 파일의 절대 경로 (api 컨테이너 기준 — 러너 컨테이너와 storageRoot를 공유하므로 그대로 사용 가능). */
    public Path storageStateFile(String projectId) {
        return Path.of(properties.storageRoot(), "auth", projectId, "storage-state.json")
                .toAbsolutePath().normalize();
    }

    /** 지금 로그인 선행 시나리오를 다시 돌려야 하는지 판단한다. 캐시가 없거나 만료됐으면 true. */
    public boolean needsRefresh(Project project) {
        if (!isConfigured(project)) {
            return false;
        }
        Path file = storageStateFile(project.getProjectId());
        if (!Files.exists(file)) {
            return true;
        }
        try {
            FileTime modified = Files.getLastModifiedTime(file);
            int maxAge = project.getStorageStateMaxAgeMinutes() != null
                    ? project.getStorageStateMaxAgeMinutes() : DEFAULT_MAX_AGE_MINUTES;
            return modified.toInstant().isBefore(Instant.now().minus(maxAge, ChronoUnit.MINUTES));
        } catch (IOException e) {
            log.warn("프로젝트 {} storageState 수정 시각 조회 실패, 재생성으로 처리: {}", project.getProjectId(), e.getMessage());
            return true;
        }
    }

    /** "지금 재생성" 버튼 등에서 호출 — 캐시를 지우면 다음 실행 때 로그인 선행 시나리오가 다시 돈다. */
    public void clear(String projectId) {
        try {
            Files.deleteIfExists(storageStateFile(projectId));
        } catch (IOException e) {
            log.warn("프로젝트 {} storageState 삭제 실패: {}", projectId, e.getMessage());
        }
    }

    public AuthStateStatusResponse status(Project project) {
        boolean configured = isConfigured(project);
        Path file = storageStateFile(project.getProjectId());
        boolean exists = configured && Files.exists(file);
        Instant updatedAt = null;
        Instant expiresAt = null;
        if (exists) {
            try {
                updatedAt = Files.getLastModifiedTime(file).toInstant();
                int maxAge = project.getStorageStateMaxAgeMinutes() != null
                        ? project.getStorageStateMaxAgeMinutes() : DEFAULT_MAX_AGE_MINUTES;
                expiresAt = updatedAt.plus(maxAge, ChronoUnit.MINUTES);
            } catch (IOException ignored) {}
        }
        return new AuthStateStatusResponse(
                configured, project.getLoginSetupSpecPath(), project.getStorageStateMaxAgeMinutes(),
                exists, updatedAt, expiresAt
        );
    }
}
