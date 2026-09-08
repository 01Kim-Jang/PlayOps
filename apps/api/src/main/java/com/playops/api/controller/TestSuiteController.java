package com.playops.api.controller;

import com.playops.api.dto.ExecutionResponse;
import com.playops.api.dto.TestSuiteRequest;
import com.playops.api.dto.TestSuiteResponse;
import com.playops.api.entity.User;
import com.playops.api.service.TestSuiteService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/test-suites")
public class TestSuiteController {

    private final TestSuiteService testSuiteService;

    public TestSuiteController(TestSuiteService testSuiteService) {
        this.testSuiteService = testSuiteService;
    }

    @GetMapping
    public List<TestSuiteResponse> list(@PathVariable String projectId) {
        return testSuiteService.listByProject(projectId);
    }

    @PostMapping
    public TestSuiteResponse create(
            @PathVariable String projectId,
            @RequestBody TestSuiteRequest body,
            HttpServletRequest request
    ) {
        User user = currentUser(request);
        return testSuiteService.create(projectId, body, user != null ? user.getId() : null);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String projectId, @PathVariable Long id) {
        testSuiteService.delete(projectId, id);
    }

    @PostMapping("/{id}/execute")
    public ExecutionResponse execute(@PathVariable String projectId, @PathVariable Long id) {
        return testSuiteService.execute(projectId, id);
    }

    private User currentUser(HttpServletRequest request) {
        return (User) request.getAttribute("currentUser");
    }
}
