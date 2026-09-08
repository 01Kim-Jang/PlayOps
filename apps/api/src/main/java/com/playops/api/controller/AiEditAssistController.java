package com.playops.api.controller;

import com.playops.api.dto.AiBootstrapRequest;
import com.playops.api.dto.AiBootstrapResponse;
import com.playops.api.dto.AiEditAssistRequest;
import com.playops.api.dto.AiEditAssistResponse;
import com.playops.api.dto.AiEditAssistVerifyRequest;
import com.playops.api.dto.AiEditAssistVerifyResponse;
import com.playops.api.service.AiBootstrapService;
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
    private final AiBootstrapService aiBootstrapService;

    public AiEditAssistController(AiEditAssistService aiEditAssistService, AiBootstrapService aiBootstrapService) {
        this.aiEditAssistService = aiEditAssistService;
        this.aiBootstrapService = aiBootstrapService;
    }

    @PostMapping("/edit-assist")
    public AiEditAssistResponse editAssist(@PathVariable String projectId, @RequestBody AiEditAssistRequest body) {
        String content = aiEditAssistService.editFile(
                projectId, body.filePath(), body.currentContent(), body.instruction());
        return new AiEditAssistResponse(content);
    }

    @PostMapping("/edit-assist/verify")
    public AiEditAssistVerifyResponse verify(@PathVariable String projectId, @RequestBody AiEditAssistVerifyRequest body) {
        return aiEditAssistService.verifyEdit(projectId, body.filePath(), body.proposedContent(), body.specPath());
    }

    /** 프로젝트 등록 직후 AI가 초기 케이스를 만들고 곧바로 실행까지 시작한다. */
    @PostMapping("/bootstrap")
    public AiBootstrapResponse bootstrap(@PathVariable String projectId, @RequestBody AiBootstrapRequest body) {
        return aiBootstrapService.bootstrap(projectId, body.instruction(), body.provider());
    }
}
