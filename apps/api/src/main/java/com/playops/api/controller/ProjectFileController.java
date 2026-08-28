package com.playops.api.controller;

import com.playops.api.service.FileStorageService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}/files")
public class ProjectFileController {

    private final FileStorageService fileStorageService;

    public ProjectFileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    public List<String> list(@PathVariable String projectId) {
        return fileStorageService.listFiles(projectId);
    }

    @PostMapping("/upload")
    public Map<String, String> uploadZip(
            @PathVariable String projectId,
            @RequestParam("file") MultipartFile file
    ) {
        fileStorageService.uploadZip(projectId, file);
        return Map.of("message", "Upload successful", "projectId", projectId);
    }

    @PostMapping("/upload-multiple")
    public Map<String, String> uploadMultiple(
            @PathVariable String projectId,
            @RequestParam("files") MultipartFile[] files
    ) {
        fileStorageService.uploadFiles(projectId, files);
        return Map.of("message", "Upload successful", "projectId", projectId);
    }
}
