package com.playops.api.repository;

import com.playops.api.entity.TestSuite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestSuiteRepository extends JpaRepository<TestSuite, Long> {
    List<TestSuite> findByProjectIdOrderByCreatedAtDesc(String projectId);
}
