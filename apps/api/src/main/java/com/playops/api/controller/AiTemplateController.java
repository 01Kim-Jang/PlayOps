package com.playops.api.controller;

import com.playops.api.dto.AiTemplateRequest;
import com.playops.api.dto.AiTemplateResponse;
import com.playops.api.service.AiTemplateService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai/templates")
public class AiTemplateController {

    private final AiTemplateService aiTemplateService;

    public AiTemplateController(AiTemplateService aiTemplateService) {
        this.aiTemplateService = aiTemplateService;
    }

    @PostMapping("/generate")
    public AiTemplateResponse generateTemplate(@RequestBody AiTemplateRequest request) {
        return aiTemplateService.generateTemplate(request);
    }
}
