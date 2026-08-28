package com.playops.api.controller;

import com.playops.api.dto.PlaywrightTemplateCloneRequest;
import com.playops.api.dto.PlaywrightTemplateRequest;
import com.playops.api.dto.PlaywrightTemplateResponse;
import com.playops.api.dto.TemplateFileRequest;
import com.playops.api.service.PlaywrightTemplateService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/templates")
public class PlaywrightTemplateController {

    private final PlaywrightTemplateService templateService;

    public PlaywrightTemplateController(PlaywrightTemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    public List<PlaywrightTemplateResponse> list() {
        return templateService.listTemplates();
    }

    @GetMapping("/{templateId}")
    public PlaywrightTemplateResponse get(@PathVariable String templateId) {
        return templateService.getTemplate(templateId);
    }

    @PostMapping
    public PlaywrightTemplateResponse create(@RequestBody PlaywrightTemplateRequest request) {
        return templateService.createTemplate(request);
    }

    @PostMapping("/{templateId}/clone")
    public PlaywrightTemplateResponse clone(
            @PathVariable String templateId,
            @RequestBody PlaywrightTemplateCloneRequest request
    ) {
        return templateService.cloneTemplate(templateId, request);
    }

    @PutMapping("/{templateId}")
    public PlaywrightTemplateResponse update(
            @PathVariable String templateId,
            @RequestBody PlaywrightTemplateRequest request
    ) {
        return templateService.updateTemplate(templateId, request);
    }

    @DeleteMapping("/{templateId}")
    public void delete(@PathVariable String templateId) {
        templateService.deleteTemplate(templateId);
    }

    @GetMapping("/{templateId}/files")
    public Map<String, String> getFile(
            @PathVariable String templateId,
            @RequestParam String path
    ) {
        return Map.of(
                "path", path,
                "content", templateService.getTemplateFileContent(templateId, path)
        );
    }

    @PutMapping("/{templateId}/files")
    public PlaywrightTemplateResponse saveFile(
            @PathVariable String templateId,
            @RequestParam String path,
            @RequestBody TemplateFileRequest request
    ) {
        return templateService.saveTemplateFile(templateId, path, request.content());
    }

    @DeleteMapping("/{templateId}/files")
    public PlaywrightTemplateResponse deleteFile(
            @PathVariable String templateId,
            @RequestParam String path
    ) {
        return templateService.deleteTemplateFile(templateId, path);
    }

    @GetMapping("/{templateId}/source.zip")
    public ResponseEntity<ByteArrayResource> downloadSource(@PathVariable String templateId) {
        byte[] zip = templateService.createTemplateZip(templateId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + templateId + "-template.zip\"")
                .contentLength(zip.length)
                .body(new ByteArrayResource(zip));
    }
}
