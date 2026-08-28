package com.playops.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.playops.api.config.PlayOpsProperties;
import com.playops.api.dto.PlaywrightTemplateCloneRequest;
import com.playops.api.dto.PlaywrightTemplateRequest;
import com.playops.api.dto.PlaywrightTemplateResponse;
import com.playops.api.dto.ScaffoldResponse;
import com.playops.api.entity.Project;
import com.playops.api.exception.ApiException;
import com.playops.api.util.RuntimeVersions;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class PlaywrightTemplateService {

    private static final String TEMPLATES_ROOT = "classpath:templates/";
    private static final String MANIFEST_FILE = "template.json";
    private static final String DELETED_DIR = ".deleted";
    private static final String TEMPLATE_ID_PATTERN = "[A-Za-z0-9][A-Za-z0-9._-]{1,79}";

    private final PlayOpsProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    public PlaywrightTemplateService(PlayOpsProperties properties) {
        this.properties = properties;
    }

    public List<PlaywrightTemplateResponse> listTemplates() {
        Map<String, PlaywrightTemplateResponse> result = new LinkedHashMap<>();

        for (PlaywrightTemplateResponse template : listBuiltinTemplates()) {
            if (!isDeleted(template.id())) {
                result.put(template.id(), template);
            }
        }

        for (PlaywrightTemplateResponse template : listCustomTemplates()) {
            if (!isDeleted(template.id())) {
                result.put(template.id(), template);
            }
        }

        return result.values().stream()
                .sorted(Comparator.comparing(PlaywrightTemplateResponse::id))
                .toList();
    }

    public PlaywrightTemplateResponse getTemplate(String templateId) {
        validateTemplateId(templateId);
        return listTemplates().stream()
                .filter(t -> t.id().equals(templateId))
                .findFirst()
                .orElseThrow(() -> new ApiException(404, "Template not found: " + templateId));
    }

    public String getTemplateFileContent(String templateId, String relativePath) {
        getTemplate(templateId);
        Path path = validateRelativePath(relativePath);
        Path customFile = templateDir(templateId).resolve(path).normalize();
        if (Files.exists(customFile) && Files.isRegularFile(customFile)) {
            try {
                return Files.readString(customFile, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new ApiException(500, "Failed to read template file: " + e.getMessage());
            }
        }

        String location = TEMPLATES_ROOT + templateId + "/" + path.toString().replace('\\', '/');
        try {
            Resource resource = resolver.getResource(location);
            if (!resource.exists()) {
                throw new ApiException(404, "Template file not found: " + relativePath);
            }
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (ApiException e) {
            throw e;
        } catch (IOException e) {
            throw new ApiException(500, "Failed to read template file: " + e.getMessage());
        }
    }

    public PlaywrightTemplateResponse createTemplate(PlaywrightTemplateRequest request) {
        String id = validateTemplateId(request.id());
        if (templateExists(id)) {
            throw new ApiException(409, "Template already exists: " + id);
        }

        Path dir = templateDir(id);
        try {
            Files.createDirectories(dir.resolve("tests"));
            writeManifest(dir, id, requiredText(request.name(), "Template name is required"), text(request.description()), false);
            Files.writeString(dir.resolve("package.json"), starterPackageJson(id), StandardCharsets.UTF_8);
            Files.writeString(dir.resolve("playwright.config.ts"), starterPlaywrightConfig(), StandardCharsets.UTF_8);
            Files.writeString(dir.resolve("tests/example.spec.ts"), starterSpec(), StandardCharsets.UTF_8);
            deleteMarker(id);
        } catch (IOException e) {
            throw new ApiException(500, "Failed to create template: " + e.getMessage());
        }
        return getTemplate(id);
    }

    public PlaywrightTemplateResponse cloneTemplate(String sourceId, PlaywrightTemplateCloneRequest request) {
        PlaywrightTemplateResponse source = getTemplate(sourceId);
        String id = validateTemplateId(request.id());
        if (templateExists(id)) {
            throw new ApiException(409, "Template already exists: " + id);
        }

        Path target = templateDir(id);
        try {
            Files.createDirectories(target);
            for (String file : source.files()) {
                Path relative = validateRelativePath(file);
                Path targetFile = target.resolve(relative).normalize();
                Files.createDirectories(targetFile.getParent());
                Files.writeString(targetFile, getTemplateFileContent(sourceId, file), StandardCharsets.UTF_8);
            }
            writeManifest(
                    target,
                    id,
                    requiredText(request.name(), "Template name is required"),
                    text(request.description()),
                    false
            );
            deleteMarker(id);
        } catch (IOException e) {
            throw new ApiException(500, "Failed to clone template: " + e.getMessage());
        }
        return getTemplate(id);
    }

    public PlaywrightTemplateResponse updateTemplate(String templateId, PlaywrightTemplateRequest request) {
        PlaywrightTemplateResponse current = getTemplate(templateId);
        Path dir = materializeTemplate(templateId);
        try {
            writeManifest(
                    dir,
                    templateId,
                    requiredText(request.name(), "Template name is required"),
                    text(request.description()),
                    current.isDefault()
            );
        } catch (IOException e) {
            throw new ApiException(500, "Failed to update template: " + e.getMessage());
        }
        return getTemplate(templateId);
    }

    public PlaywrightTemplateResponse saveTemplateFile(String templateId, String relativePath, String content) {
        getTemplate(templateId);
        Path path = validateRelativePath(relativePath);
        if (MANIFEST_FILE.equals(path.toString().replace('\\', '/'))) {
            throw new ApiException(400, "template.json is managed by template settings");
        }

        Path dir = materializeTemplate(templateId);
        Path file = dir.resolve(path).normalize();
        ensureInside(dir, file);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, content != null ? content : "", StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ApiException(500, "Failed to save template file: " + e.getMessage());
        }
        return getTemplate(templateId);
    }

    public PlaywrightTemplateResponse deleteTemplateFile(String templateId, String relativePath) {
        getTemplate(templateId);
        Path path = validateRelativePath(relativePath);
        if (MANIFEST_FILE.equals(path.toString().replace('\\', '/'))) {
            throw new ApiException(400, "template.json cannot be deleted");
        }

        Path dir = materializeTemplate(templateId);
        Path file = dir.resolve(path).normalize();
        ensureInside(dir, file);
        try {
            Files.deleteIfExists(file);
            pruneEmptyParents(file.getParent(), dir);
        } catch (IOException e) {
            throw new ApiException(500, "Failed to delete template file: " + e.getMessage());
        }
        return getTemplate(templateId);
    }

    public void deleteTemplate(String templateId) {
        PlaywrightTemplateResponse template = getTemplate(templateId);
        if (template.isDefault()) {
            throw new ApiException(400, "Default template cannot be deleted");
        }

        Path dir = templateDir(templateId);
        if (Files.exists(dir)) {
            deleteDirectory(dir);
        }

        if (isBuiltinTemplate(templateId)) {
            try {
                Files.createDirectories(deletedRoot());
                Files.writeString(deletedRoot().resolve(templateId), "deleted", StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new ApiException(500, "Failed to mark template deleted: " + e.getMessage());
            }
        }
    }

    public byte[] createTemplateZip(String templateId) {
        PlaywrightTemplateResponse template = getTemplate(templateId);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            addZipEntry(zip, MANIFEST_FILE, manifestJson(
                    template.id(),
                    template.name(),
                    template.description(),
                    template.isDefault()
            ));
            for (String file : template.files()) {
                addZipEntry(zip, file, getTemplateFileContent(templateId, file));
            }
            zip.finish();
            return out.toByteArray();
        } catch (IOException e) {
            throw new ApiException(500, "Failed to create template zip: " + e.getMessage());
        }
    }

    public ScaffoldResponse scaffold(Path projectPath, Project project, String templateId, boolean overwrite) {
        PlaywrightTemplateResponse template = getTemplate(templateId);

        if (!overwrite && hasExistingSource(projectPath)) {
            return new ScaffoldResponse(
                    project.getProjectId(),
                    templateId,
                    List.of(),
                    true
            );
        }

        if (overwrite && Files.exists(projectPath)) {
            clearProjectFiles(projectPath);
        }

        try {
            Files.createDirectories(projectPath);
        } catch (IOException e) {
            throw new ApiException(500, "Failed to create project directory: " + e.getMessage());
        }

        Map<String, String> variables = buildVariables(project);
        List<String> created = new ArrayList<>();

        try {
            for (String file : template.files()) {
                Path target = projectPath.resolve(file);
                Files.createDirectories(target.getParent());
                String content = render(getTemplateFileContent(templateId, file), variables);
                Files.writeString(target, content, StandardCharsets.UTF_8);
                created.add(file);
            }
        } catch (IOException e) {
            throw new ApiException(500, "Failed to scaffold project: " + e.getMessage());
        }

        created.sort(String::compareTo);
        return new ScaffoldResponse(project.getProjectId(), templateId, created, false);
    }

    /**
     * GitHub 레포 clone 직후 호출한다. 이미 Playwright 설정(playwright.config.ts/js)이 있으면
     * 아무것도 하지 않는다. 없으면 템플릿의 Playwright 관련 파일만 "없는 것만" 채워 넣는다 —
     * {@link #scaffold}와 달리 기존 앱 소스를 지우거나 통째로 건너뛰지 않고, 파일 단위로 병합한다.
     * package.json은 이미 있으면 devDependencies에 @playwright/test만 추가하고 나머지는 그대로 둔다.
     * @return 실제로 뭔가 채워 넣었으면 true, 이미 Playwright가 있어서 아무 것도 안 했으면 false
     */
    public boolean ensurePlaywrightScaffold(Path projectPath, Project project, String templateId) {
        if (hasPlaywrightConfig(projectPath)) {
            return false;
        }

        PlaywrightTemplateResponse template = getTemplate(templateId);
        Map<String, String> variables = buildVariables(project);

        try {
            for (String file : template.files()) {
                if ("package.json".equals(file)) {
                    mergePlaywrightIntoPackageJson(projectPath.resolve("package.json"), project);
                    continue;
                }
                Path target = projectPath.resolve(file);
                if (Files.exists(target)) {
                    continue;
                }
                Files.createDirectories(target.getParent());
                String content = render(getTemplateFileContent(templateId, file), variables);
                Files.writeString(target, content, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new ApiException(500, "Failed to merge Playwright template into cloned repository: " + e.getMessage());
        }
        return true;
    }

    private boolean hasPlaywrightConfig(Path projectPath) {
        return Files.exists(projectPath.resolve("playwright.config.ts"))
                || Files.exists(projectPath.resolve("playwright.config.js"));
    }

    private void mergePlaywrightIntoPackageJson(Path packageJsonPath, Project project) {
        try {
            ObjectNode root;
            if (Files.exists(packageJsonPath)) {
                JsonNode parsed = objectMapper.readTree(Files.readString(packageJsonPath, StandardCharsets.UTF_8));
                if (!parsed.isObject()) {
                    throw new ApiException(500, "package.json이 올바른 JSON 객체가 아닙니다.");
                }
                root = (ObjectNode) parsed;
            } else {
                root = objectMapper.createObjectNode();
                root.put("name", project.getProjectId());
                root.put("private", true);
            }

            ObjectNode devDeps = (root.has("devDependencies") && root.get("devDependencies").isObject())
                    ? (ObjectNode) root.get("devDependencies")
                    : root.putObject("devDependencies");
            if (!devDeps.has("@playwright/test")) {
                devDeps.put("@playwright/test", project.getPlaywrightVersion());
            }

            Files.writeString(
                    packageJsonPath,
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root),
                    StandardCharsets.UTF_8
            );
        } catch (IOException e) {
            throw new ApiException(500, "Failed to update package.json for Playwright: " + e.getMessage());
        }
    }

    public String resolveTemplateId(String templateId) {
        if (templateId == null || templateId.isBlank()) {
            return listTemplates().stream()
                    .filter(PlaywrightTemplateResponse::isDefault)
                    .map(PlaywrightTemplateResponse::id)
                    .findFirst()
                    .orElse("default");
        }
        getTemplate(templateId);
        return templateId;
    }

    private List<PlaywrightTemplateResponse> listBuiltinTemplates() {
        try {
            Resource[] manifests = resolver.getResources(TEMPLATES_ROOT + "*/" + MANIFEST_FILE);
            List<PlaywrightTemplateResponse> result = new ArrayList<>();
            for (Resource manifest : manifests) {
                result.add(loadBuiltinManifest(manifest));
            }
            return result;
        } catch (IOException e) {
            throw new ApiException(500, "Failed to load templates: " + e.getMessage());
        }
    }

    private List<PlaywrightTemplateResponse> listCustomTemplates() {
        Path root = templatesRoot();
        if (!Files.exists(root)) {
            return List.of();
        }

        try (Stream<Path> stream = Files.list(root)) {
            return stream
                    .filter(Files::isDirectory)
                    .filter(path -> !DELETED_DIR.equals(path.getFileName().toString()))
                    .filter(path -> Files.exists(path.resolve(MANIFEST_FILE)))
                    .map(this::loadCustomManifest)
                    .sorted(Comparator.comparing(PlaywrightTemplateResponse::id))
                    .toList();
        } catch (IOException e) {
            throw new ApiException(500, "Failed to load custom templates: " + e.getMessage());
        }
    }

    private PlaywrightTemplateResponse loadBuiltinManifest(Resource manifest) throws IOException {
        JsonNode root = objectMapper.readTree(manifest.getInputStream());
        String id = root.path("id").asText();
        return new PlaywrightTemplateResponse(
                id,
                root.path("name").asText(),
                root.path("description").asText(""),
                root.path("default").asBoolean(false),
                listBuiltinTemplateFilePaths(id),
                false,
                true,
                !root.path("default").asBoolean(false)
        );
    }

    private PlaywrightTemplateResponse loadCustomManifest(Path dir) {
        try {
            JsonNode root = objectMapper.readTree(dir.resolve(MANIFEST_FILE).toFile());
            String id = root.path("id").asText(dir.getFileName().toString());
            boolean isDefault = root.path("default").asBoolean(false);
            return new PlaywrightTemplateResponse(
                    id,
                    root.path("name").asText(),
                    root.path("description").asText(""),
                    isDefault,
                    listCustomTemplateFilePaths(dir),
                    true,
                    true,
                    !isDefault
            );
        } catch (IOException e) {
            throw new ApiException(500, "Failed to load custom template: " + e.getMessage());
        }
    }

    private List<String> listBuiltinTemplateFilePaths(String templateId) throws IOException {
        Resource[] files = resolver.getResources(TEMPLATES_ROOT + templateId + "/**");
        List<String> paths = new ArrayList<>();
        for (Resource file : files) {
            String path = toRelativeTemplatePath(templateId, file);
            if (path != null && !path.isBlank() && !MANIFEST_FILE.equals(path)) {
                paths.add(path);
            }
        }
        paths.sort(String::compareTo);
        return paths;
    }

    private List<String> listCustomTemplateFilePaths(Path dir) throws IOException {
        try (Stream<Path> walk = Files.walk(dir)) {
            return walk
                    .filter(Files::isRegularFile)
                    .map(dir::relativize)
                    .map(Path::toString)
                    .map(path -> path.replace('\\', '/'))
                    .filter(path -> !MANIFEST_FILE.equals(path))
                    .sorted()
                    .toList();
        }
    }

    private Path materializeTemplate(String templateId) {
        PlaywrightTemplateResponse template = getTemplate(templateId);
        Path dir = templateDir(templateId);
        if (Files.exists(dir.resolve(MANIFEST_FILE))) {
            return dir;
        }

        try {
            Files.createDirectories(dir);
            writeManifest(dir, template.id(), template.name(), template.description(), template.isDefault());
            for (String file : template.files()) {
                Path relative = validateRelativePath(file);
                Path target = dir.resolve(relative).normalize();
                Files.createDirectories(target.getParent());
                Files.writeString(target, getTemplateFileContent(templateId, file), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new ApiException(500, "Failed to prepare editable template: " + e.getMessage());
        }
        return dir;
    }

    private boolean templateExists(String templateId) {
        return listTemplates().stream().anyMatch(template -> template.id().equals(templateId));
    }

    private boolean isBuiltinTemplate(String templateId) {
        return listBuiltinTemplates().stream().anyMatch(template -> template.id().equals(templateId));
    }

    private boolean isDeleted(String templateId) {
        return Files.exists(deletedRoot().resolve(templateId));
    }

    private void deleteMarker(String templateId) throws IOException {
        Files.deleteIfExists(deletedRoot().resolve(templateId));
    }

    private void writeManifest(Path dir, String id, String name, String description, boolean isDefault) throws IOException {
        Files.writeString(dir.resolve(MANIFEST_FILE), manifestJson(id, name, description, isDefault), StandardCharsets.UTF_8);
    }

    private String manifestJson(String id, String name, String description, boolean isDefault) throws IOException {
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("id", id);
        manifest.put("name", name);
        manifest.put("description", description != null ? description : "");
        manifest.put("default", isDefault);
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(manifest) + "\n";
    }

    private String toRelativeTemplatePath(String templateId, Resource resource) throws IOException {
        String url = resource.getURI().toString();
        String marker = "/templates/" + templateId + "/";
        int idx = url.indexOf(marker);
        if (idx < 0) {
            return null;
        }
        String relative = url.substring(idx + marker.length());
        int query = relative.indexOf('!');
        if (query > 0) {
            relative = relative.substring(0, query);
        }
        if (relative.isBlank() || relative.endsWith("/")) {
            return null;
        }
        return relative;
    }

    private Map<String, String> buildVariables(Project project) {
        String baseUrl = project.getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://example.com";
        }
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("projectId", project.getProjectId());
        vars.put("projectName", project.getProjectName());
        vars.put("nodeVersion", RuntimeVersions.NODE_VERSION);
        vars.put("playwrightVersion", RuntimeVersions.PLAYWRIGHT_VERSION);
        vars.put("baseUrl", baseUrl);
        vars.put("packageManager", project.getPackageManager().name().toLowerCase());
        return vars;
    }

    private String render(String template, Map<String, String> variables) {
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }

    private boolean hasExistingSource(Path projectPath) throws ApiException {
        if (!Files.exists(projectPath)) {
            return false;
        }
        try {
            if (Files.exists(projectPath.resolve("package.json"))) {
                return true;
            }
            try (Stream<Path> walk = Files.walk(projectPath)) {
                return walk.anyMatch(p -> Files.isRegularFile(p) && !p.getFileName().toString().startsWith("."));
            }
        } catch (IOException e) {
            throw new ApiException(500, "Failed to check project files: " + e.getMessage());
        }
    }

    private void clearProjectFiles(Path projectPath) {
        try (Stream<Path> walk = Files.walk(projectPath)) {
            walk.sorted(Comparator.reverseOrder())
                    .filter(p -> !p.equals(projectPath))
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ignored) {}
                    });
        } catch (IOException e) {
            throw new ApiException(500, "Failed to clear project files: " + e.getMessage());
        }
    }

    private String validateTemplateId(String templateId) {
        if (templateId == null || !templateId.matches(TEMPLATE_ID_PATTERN)) {
            throw new ApiException(400, "Template id must be 2-80 characters and use letters, numbers, dot, dash, or underscore");
        }
        return templateId;
    }

    private Path validateRelativePath(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new ApiException(400, "File path is required");
        }
        String normalized = relativePath.replace('\\', '/');
        Path path = Path.of(normalized).normalize();
        String normalizedPath = path.toString();
        if (
                path.isAbsolute()
                        || normalizedPath.isBlank()
                        || ".".equals(normalizedPath)
                        || normalizedPath.equals("..")
                        || normalizedPath.startsWith(".." + java.io.File.separator)
                        || normalized.startsWith("/")
                        || normalized.contains("../")
        ) {
            throw new ApiException(400, "Invalid file path");
        }
        return path;
    }

    private void ensureInside(Path root, Path target) {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path normalizedTarget = target.toAbsolutePath().normalize();
        if (!normalizedTarget.startsWith(normalizedRoot)) {
            throw new ApiException(400, "Invalid file path");
        }
    }

    private String requiredText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ApiException(400, message);
        }
        return value.trim();
    }

    private String text(String value) {
        return value != null ? value.trim() : "";
    }

    private Path templatesRoot() {
        return Path.of(properties.storageRoot(), "templates").toAbsolutePath().normalize();
    }

    private Path deletedRoot() {
        return templatesRoot().resolve(DELETED_DIR).normalize();
    }

    private Path templateDir(String templateId) {
        validateTemplateId(templateId);
        Path dir = templatesRoot().resolve(templateId).normalize();
        ensureInside(templatesRoot(), dir);
        return dir;
    }

    private void deleteDirectory(Path dir) {
        ensureInside(templatesRoot(), dir);
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {}
            });
        } catch (IOException e) {
            throw new ApiException(500, "Failed to delete template: " + e.getMessage());
        }
    }

    private void pruneEmptyParents(Path start, Path stop) throws IOException {
        Path current = start;
        while (current != null && !current.equals(stop)) {
            try (Stream<Path> children = Files.list(current)) {
                if (children.findAny().isPresent()) {
                    return;
                }
            }
            Files.deleteIfExists(current);
            current = current.getParent();
        }
    }

    private void addZipEntry(ZipOutputStream zip, String path, String content) throws IOException {
        ZipEntry entry = new ZipEntry(path.replace('\\', '/'));
        zip.putNextEntry(entry);
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private String starterPackageJson(String templateId) {
        return """
                {
                  "name": "%s",
                  "version": "0.1.0",
                  "private": true,
                  "scripts": {
                    "test": "playwright test"
                  },
                  "devDependencies": {
                    "@playwright/test": "{{playwrightVersion}}"
                  }
                }
                """.formatted(templateId);
    }

    private String starterPlaywrightConfig() {
        return """
                import { defineConfig, devices } from '@playwright/test';

                export default defineConfig({
                  testDir: './tests',
                  timeout: 30_000,
                  use: {
                    baseURL: process.env.BASE_URL || '{{baseUrl}}',
                    trace: 'on-first-retry',
                  },
                  projects: [
                    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
                  ],
                });
                """;
    }

    private String starterSpec() {
        return """
                import { expect, test } from '@playwright/test';

                test('home page is reachable', async ({ page }) => {
                  await page.goto('/');
                  await expect(page).toHaveURL(/.+/);
                });
                """;
    }
}
