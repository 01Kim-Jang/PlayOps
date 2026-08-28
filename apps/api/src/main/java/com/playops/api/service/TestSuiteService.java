package com.playops.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playops.api.dto.TestSuiteRequest;
import com.playops.api.dto.TestSuiteResponse;
import com.playops.api.entity.TestSuite;
import com.playops.api.exception.ApiException;
import com.playops.api.repository.TestSuiteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TestSuiteService {

    private final TestSuiteRepository testSuiteRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TestSuiteService(TestSuiteRepository testSuiteRepository) {
        this.testSuiteRepository = testSuiteRepository;
    }

    public List<TestSuiteResponse> listByProject(String projectId) {
        return testSuiteRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(TestSuiteResponse::from)
                .toList();
    }

    @Transactional
    public TestSuiteResponse create(String projectId, TestSuiteRequest request, Long createdBy) {
        if (request.name() == null || request.name().isBlank()) {
            throw new ApiException(400, "묶음 이름을 입력하세요.");
        }
        if (request.specPaths() == null || request.specPaths().isEmpty()) {
            throw new ApiException(400, "케이스를 하나 이상 선택하세요.");
        }

        TestSuite suite = new TestSuite();
        suite.setProjectId(projectId);
        suite.setName(request.name().trim());
        suite.setGrep(request.grep());
        suite.setCaseCount(request.caseCount() != null ? request.caseCount() : request.specPaths().size());
        suite.setSequential(Boolean.TRUE.equals(request.sequential()));
        suite.setCreatedBy(createdBy);
        try {
            suite.setSpecPaths(objectMapper.writeValueAsString(request.specPaths()));
        } catch (Exception e) {
            throw new ApiException(500, "specPaths 직렬화 실패: " + e.getMessage());
        }
        return TestSuiteResponse.from(testSuiteRepository.save(suite));
    }

    @Transactional
    public void delete(String projectId, Long id) {
        TestSuite suite = testSuiteRepository.findById(id)
                .orElseThrow(() -> new ApiException(404, "Test suite not found"));
        if (!suite.getProjectId().equals(projectId)) {
            throw new ApiException(404, "Test suite not found");
        }
        testSuiteRepository.delete(suite);
    }
}
