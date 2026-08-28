package com.playops.api.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playops.api.entity.TestSuite;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public record TestSuiteResponse(
        Long id,
        String projectId,
        String name,
        List<String> specPaths,
        String grep,
        Integer caseCount,
        boolean sequential,
        Instant createdAt
) {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static TestSuiteResponse from(TestSuite suite) {
        List<String> paths = new ArrayList<>();
        if (suite.getSpecPaths() != null && !suite.getSpecPaths().isBlank()) {
            try {
                MAPPER.readTree(suite.getSpecPaths()).forEach(node -> paths.add(node.asText()));
            } catch (Exception ignored) {}
        }
        return new TestSuiteResponse(
                suite.getId(),
                suite.getProjectId(),
                suite.getName(),
                paths,
                suite.getGrep(),
                suite.getCaseCount(),
                suite.isSequential(),
                suite.getCreatedAt()
        );
    }
}
