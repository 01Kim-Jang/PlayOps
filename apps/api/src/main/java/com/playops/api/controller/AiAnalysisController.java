package com.playops.api.controller;

import com.playops.api.dto.AiAnalysisRequest;
import com.playops.api.dto.AiAnalysisResponse;
import com.playops.api.dto.AiChatRequest;
import com.playops.api.service.AiAnalysisService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiAnalysisController {

    private final AiAnalysisService aiAnalysisService;

    public AiAnalysisController(AiAnalysisService aiAnalysisService) {
        this.aiAnalysisService = aiAnalysisService;
    }

    @PostMapping("/analyze")
    public ResponseEntity<AiAnalysisResponse> analyze(@RequestBody AiAnalysisRequest request) {
        AiAnalysisResponse response = aiAnalysisService.analyze(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chat(@RequestBody AiChatRequest request) {
        String answer = aiAnalysisService.chat(request);
        return ResponseEntity.ok(Map.of("answer", answer));
    }
}
