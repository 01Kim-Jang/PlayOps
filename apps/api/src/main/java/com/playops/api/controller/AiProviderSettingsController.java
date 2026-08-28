package com.playops.api.controller;

import com.playops.api.dto.AiProviderSettingsRequest;
import com.playops.api.dto.AiProviderSettingsResponse;
import com.playops.api.entity.User;
import com.playops.api.entity.UserRole;
import com.playops.api.exception.ApiException;
import com.playops.api.service.AiProviderSettingsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/ai-provider-settings")
public class AiProviderSettingsController {

    private final AiProviderSettingsService aiProviderSettingsService;

    public AiProviderSettingsController(AiProviderSettingsService aiProviderSettingsService) {
        this.aiProviderSettingsService = aiProviderSettingsService;
    }

    @GetMapping
    public AiProviderSettingsResponse get(HttpServletRequest request) {
        requireAdmin(request);
        return aiProviderSettingsService.getMaskedSettings();
    }

    @PutMapping
    public AiProviderSettingsResponse update(@RequestBody AiProviderSettingsRequest body, HttpServletRequest request) {
        requireAdmin(request);
        return aiProviderSettingsService.updateSettings(body);
    }

    private void requireAdmin(HttpServletRequest request) {
        User user = (User) request.getAttribute("currentUser");
        if (user == null || user.getRole() != UserRole.ADMIN) {
            throw new ApiException(403, "Admin access required");
        }
    }
}
