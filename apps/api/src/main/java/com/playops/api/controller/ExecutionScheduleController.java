package com.playops.api.controller;

import com.playops.api.dto.ExecutionScheduleRequest;
import com.playops.api.dto.ExecutionScheduleResponse;
import com.playops.api.entity.User;
import com.playops.api.service.ExecutionScheduleService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/schedules")
public class ExecutionScheduleController {

    private final ExecutionScheduleService scheduleService;

    public ExecutionScheduleController(ExecutionScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping
    public List<ExecutionScheduleResponse> list(@PathVariable String projectId) {
        return scheduleService.listByProject(projectId);
    }

    @PostMapping
    public ExecutionScheduleResponse create(
            @PathVariable String projectId,
            @RequestBody ExecutionScheduleRequest body,
            HttpServletRequest request
    ) {
        User user = currentUser(request);
        return scheduleService.create(projectId, body, user != null ? user.getId() : null);
    }

    @PutMapping("/{id}")
    public ExecutionScheduleResponse update(@PathVariable String projectId, @PathVariable Long id, @RequestBody ExecutionScheduleRequest body) {
        return scheduleService.update(id, body);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String projectId, @PathVariable Long id) {
        scheduleService.delete(id);
    }

    private User currentUser(HttpServletRequest request) {
        return (User) request.getAttribute("currentUser");
    }
}
