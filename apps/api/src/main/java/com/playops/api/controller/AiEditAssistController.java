package com.playops.api.controller;

import com.playops.api.dto.AiEditAssistRequest;
import com.playops.api.dto.AiEditAssistResponse;
import com.playops.api.service.AiEditAssistService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/ai")
public class AiEditAssistController {

    private final AiEditAssistService aiEditAssistService;

    public AiEditAssistController(AiEditAssistService aiEditAssistService) {
        this.aiEditAssistService = aiEditAssistService;
    }

    @PostMapping("/edit-assist")
    public AiEditAssistResponse editAssist(@PathVariable String projectId, @RequestBody AiEditAssistRequest body) {
        String content = aiEditAssistService.editFile(
                projectId, body.filePath(), body.currentContent(), body.instruction());
        return new AiEditAssistResponse(content);
    }
}
