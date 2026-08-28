package com.playops.api.controller;

import com.playops.api.dto.SlackSettingsRequest;
import com.playops.api.dto.SlackSettingsResponse;
import com.playops.api.entity.User;
import com.playops.api.entity.UserRole;
import com.playops.api.exception.ApiException;
import com.playops.api.service.SlackNotificationService;
import com.playops.api.service.SlackSettingsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/slack-settings")
public class SlackSettingsController {

    private final SlackSettingsService slackSettingsService;
    private final SlackNotificationService slackNotificationService;

    public SlackSettingsController(SlackSettingsService slackSettingsService, SlackNotificationService slackNotificationService) {
        this.slackSettingsService = slackSettingsService;
        this.slackNotificationService = slackNotificationService;
    }

    @GetMapping
    public SlackSettingsResponse get(HttpServletRequest request) {
        requireAdmin(request);
        return slackSettingsService.getMaskedSettings();
    }

    @PutMapping
    public SlackSettingsResponse update(@RequestBody SlackSettingsRequest body, HttpServletRequest request) {
        requireAdmin(request);
        return slackSettingsService.updateSettings(body);
    }

    @PostMapping("/test")
    public void test(HttpServletRequest request) {
        requireAdmin(request);
        slackNotificationService.sendTest();
    }

    private void requireAdmin(HttpServletRequest request) {
        User user = (User) request.getAttribute("currentUser");
        if (user == null || user.getRole() != UserRole.ADMIN) {
            throw new ApiException(403, "Admin access required");
        }
    }
}
