package com.playops.api.config;

import com.playops.api.entity.User;
import com.playops.api.entity.UserRole;
import com.playops.api.entity.Project;
import com.playops.api.entity.PackageManager;
import com.playops.api.entity.ProjectServerType;
import com.playops.api.entity.RunnerLifecycle;
import com.playops.api.repository.UserRepository;
import com.playops.api.repository.ProjectRepository;
import com.playops.api.util.RuntimeVersions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(10)
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final PasswordEncoder passwordEncoder;
    private final String loginUsername;
    private final String loginPassword;

    public DataLoader(
            UserRepository userRepository,
            ProjectRepository projectRepository,
            PasswordEncoder passwordEncoder,
            @Value("${LOGIN_USERNAME:admin}") String loginUsername,
            @Value("${LOGIN_PASSWORD:admin}") String loginPassword
    ) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginUsername = loginUsername;
        this.loginPassword = loginPassword;
    }

    @Override
    public void run(String... args) {
        User existingAdmin = userRepository.findByUsername(loginUsername).orElse(null);
        if (existingAdmin == null) {
            User admin = new User();
            admin.setUsername(loginUsername);
            admin.setPassword(passwordEncoder.encode(loginPassword));
            admin.setRole(UserRole.ADMIN);
            userRepository.save(admin);
        } else if (!isBcryptHash(existingAdmin.getPassword()) && existingAdmin.getPassword().equals(loginPassword)) {
            // 과거 평문으로 생성된 부트스트랩 admin 계정을 최초 재기동 시 1회 해시로 마이그레이션한다.
            existingAdmin.setPassword(passwordEncoder.encode(loginPassword));
            userRepository.save(existingAdmin);
        }

        removeObsoleteDemoProject("kakao.com");
        normalizeExistingRuntimeVersions();

        seedDemoProject(10, "example.com", "Example.com Demo", ProjectServerType.DEV, "https://example.com", RuntimeVersions.NODE_VERSION, RuntimeVersions.PLAYWRIGHT_VERSION, RunnerLifecycle.EPHEMERAL, "정적 샘플 사이트로 기본 접속, 링크 이동, 콘텐츠 확인을 검증합니다.", "PlayOps 기본 데모와 시나리오 파싱 검증");
        seedDemoProject(20, "google.com", "Google Demo", ProjectServerType.TEST, "https://www.google.com", RuntimeVersions.NODE_VERSION, RuntimeVersions.PLAYWRIGHT_VERSION, RunnerLifecycle.EPHEMERAL, "검색 포털 데모 프로젝트입니다.", "검색 입력과 기본 화면 요소 검증");
        seedDemoProject(40, "naver.com", "Naver Demo", ProjectServerType.PROD, "https://www.naver.com", RuntimeVersions.NODE_VERSION, RuntimeVersions.PLAYWRIGHT_VERSION, RunnerLifecycle.EPHEMERAL, "운영 포털 성격의 데모 프로젝트입니다.", "검색 입력, 메뉴 이동, 콘텐츠 확인 검증");
        seedDemoProject(50, "npims-stg.skax.co.kr", "NPIMS STG", ProjectServerType.TEST, "https://npims-stg.skax.co.kr", RuntimeVersions.NODE_VERSION, RuntimeVersions.PLAYWRIGHT_VERSION, RunnerLifecycle.PERSISTENT, "NPIMS 스테이징 환경 프로젝트입니다.", "스테이징 환경의 주요 화면 접속과 기본 동작 검증");
    }

    private static boolean isBcryptHash(String value) {
        return value != null
                && (value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$"));
    }

    private void removeObsoleteDemoProject(String projectId) {
        if (projectRepository.existsById(projectId)) {
            projectRepository.deleteById(projectId);
        }
    }

    private void seedDemoProject(
            Integer displayOrder,
            String projectId,
            String projectName,
            ProjectServerType serverType,
            String baseUrl,
            String nodeVersion,
            String playwrightVersion,
            RunnerLifecycle runnerLifecycle,
            String description,
            String testPurpose
    ) {
        Project project = projectRepository.findById(projectId).orElseGet(Project::new);
        boolean isNew = project.getProjectId() == null;
        if (!isNew) {
            migrateDemoDefaults(project);
            projectRepository.save(project);
            return;
        }
        project.setProjectId(projectId);
        project.setProjectName(projectName);
        project.setDisplayOrder(displayOrder);
        project.setServerType(serverType);
        project.setDescription(description);
        project.setTestPurpose(testPurpose);
        project.setManagerName("PlayOps 관리자");
        project.setManagerContact("admin@example.com");
        project.setNodeVersion(RuntimeVersions.NODE_VERSION);
        project.setPlaywrightVersion(RuntimeVersions.PLAYWRIGHT_VERSION);
        project.setPackageManager(PackageManager.PNPM);
        project.setInstallCommand("pnpm install");
        project.setTestCommand("npx playwright test --project=chromium");
        project.setWorkingDirectory(".");
        if (isNew || project.getEnvVariables() == null || project.getEnvVariables().isBlank()) {
            project.setEnvVariables("{}");
        }
        project.setTimeout(300);
        project.setParallelLimit(1);
        project.setBaseUrl(baseUrl);
        project.setRunnerLifecycle(runnerLifecycle);
        if (isNew) {
            project.setDockerEnabled(false);
        }
        projectRepository.save(project);
    }

    private void migrateDemoDefaults(Project project) {
        project.setNodeVersion(RuntimeVersions.NODE_VERSION);
        project.setPlaywrightVersion(RuntimeVersions.PLAYWRIGHT_VERSION);
        if (project.getEnvVariables() == null || project.getEnvVariables().isBlank()) {
            project.setEnvVariables("{}");
        }
    }

    private void normalizeExistingRuntimeVersions() {
        for (Project project : projectRepository.findAll()) {
            boolean changed = false;
            if (!RuntimeVersions.isAllowedNodeVersion(project.getNodeVersion())) {
                project.setNodeVersion(RuntimeVersions.NODE_VERSION);
                changed = true;
            }
            if (!RuntimeVersions.isAllowedPlaywrightVersion(project.getPlaywrightVersion())) {
                project.setPlaywrightVersion(RuntimeVersions.PLAYWRIGHT_VERSION);
                changed = true;
            }
            if (changed) {
                projectRepository.save(project);
            }
        }
    }
}
