package com.playops.api.controller;

import com.playops.api.dto.ServiceHealthResponse;
import com.playops.api.service.ServiceHealthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final ServiceHealthService serviceHealthService;

    public HealthController(ServiceHealthService serviceHealthService) {
        this.serviceHealthService = serviceHealthService;
    }

    @GetMapping("/services")
    public ServiceHealthResponse services() {
        return serviceHealthService.check();
    }
}
