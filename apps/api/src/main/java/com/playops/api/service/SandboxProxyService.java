package com.playops.api.service;

import com.playops.api.config.PlayOpsProperties;
import com.playops.api.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * GitHub clone 전용 격리 네트워크(internal, egress 없음)와, 그 네트워크에서만 도달 가능한
 * allowlist 프록시 사이드카를 관리한다. 실제 Playwright 테스트를 실행하는 러너 컨테이너는
 * 건드리지 않는다 — 테스트 대상은 임의의 baseUrl일 수 있어 러너 네트워크까지 제한하면
 * 정상적인 E2E 테스트 실행이 깨지기 때문에, clone 전용 1회성 컨테이너만 이 네트워크를 쓴다.
 */
@Service
public class SandboxProxyService {

    private static final Logger log = LoggerFactory.getLogger(SandboxProxyService.class);

    public static final String SANDBOX_NETWORK = "playops-github-sandbox";
    private static final String PROXY_CONTAINER = "playops-github-proxy";
    private static final String PROXY_IMAGE = "playops-sandbox-proxy:latest";
    public static final String PROXY_HOST = PROXY_CONTAINER;
    public static final int PROXY_PORT = 3128;

    private static final String ALLOWED_HOSTS =
            "github.com,api.github.com,codeload.github.com,*.githubusercontent.com";

    private final PlayOpsProperties properties;
    private final ReentrantLock setupLock = new ReentrantLock();
    private volatile boolean ready = false;

    public SandboxProxyService(PlayOpsProperties properties) {
        this.properties = properties;
    }

    /** 네트워크 + 프록시 컨테이너가 준비되어 있는지 확인하고, 없으면 생성한다. 여러 프로젝트가 공유한다. */
    public void ensureReady() {
        if (ready) {
            return;
        }
        setupLock.lock();
        try {
            if (ready) {
                return;
            }
            if (!properties.dockerEnabled()) {
                throw new ApiException(409, "Docker가 비활성화되어 있어 GitHub 샌드박스를 준비할 수 없습니다.");
            }
            ensureNetwork();
            ensureProxyImage();
            ensureProxyContainer();
            ready = true;
        } finally {
            setupLock.unlock();
        }
    }

    private void ensureNetwork() {
        if (runQuiet(List.of("docker", "network", "inspect", SANDBOX_NETWORK), 10)) {
            return;
        }
        log.info("Creating GitHub sandbox network {}", SANDBOX_NETWORK);
        run(List.of("docker", "network", "create", "--internal", SANDBOX_NETWORK), 15);
    }

    private void ensureProxyImage() {
        if (runQuiet(List.of("docker", "image", "inspect", PROXY_IMAGE), 10)) {
            return;
        }
        log.info("Building GitHub sandbox proxy image {}", PROXY_IMAGE);
        run(List.of(
                "docker", "build",
                "--force-rm",
                "-f", "/app/sandbox-proxy.Dockerfile",
                "-t", PROXY_IMAGE,
                "/app"
        ), 300);
    }

    private void ensureProxyContainer() {
        if (runQuiet(List.of("docker", "inspect", "-f", "{{.State.Running}}", PROXY_CONTAINER), 10)) {
            return;
        }
        removeQuiet(PROXY_CONTAINER);
        log.info("Starting GitHub sandbox proxy container {}", PROXY_CONTAINER);
        run(List.of(
                "docker", "run", "-d",
                "--name", PROXY_CONTAINER,
                "--restart", "unless-stopped",
                "--network", SANDBOX_NETWORK,
                "-e", "ALLOWED_HOSTS=" + ALLOWED_HOSTS,
                PROXY_IMAGE
        ), 30);
        // 프록시는 internal 네트워크에만 붙어 있으면 자기 자신도 외부 인터넷에 못 나가므로,
        // 실제 인터넷 egress가 가능한 기본 bridge 네트워크에도 추가로 연결한다.
        run(List.of("docker", "network", "connect", "bridge", PROXY_CONTAINER), 15);
    }

    private boolean runQuiet(List<String> command, int timeoutSeconds) {
        try {
            run(command, timeoutSeconds);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void removeQuiet(String containerName) {
        try {
            run(List.of("docker", "rm", "-f", containerName), 20);
        } catch (Exception ignored) {
        }
    }

    private String run(List<String> command, int timeoutSeconds) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new ApiException(500, "Sandbox 명령이 시간 초과되었습니다: " + String.join(" ", command));
            }
            if (process.exitValue() != 0) {
                throw new ApiException(500, output.toString().trim());
            }
            return output.toString().trim();
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(500, "Sandbox 명령 실행 실패: " + e.getMessage());
        }
    }
}
