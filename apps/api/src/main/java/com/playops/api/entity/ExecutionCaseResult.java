package com.playops.api.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "execution_case_results")
public class ExecutionCaseResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "execution_id", nullable = false)
    private Long executionId;

    @Column(name = "spec_path", length = 500)
    private String specPath;

    @Column(name = "case_title", columnDefinition = "TEXT")
    private String caseTitle;

    /** PASSED / FAILED / SKIPPED */
    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() { return id; }

    public Long getExecutionId() { return executionId; }
    public void setExecutionId(Long executionId) { this.executionId = executionId; }

    public String getSpecPath() { return specPath; }
    public void setSpecPath(String specPath) { this.specPath = specPath; }

    public String getCaseTitle() { return caseTitle; }
    public void setCaseTitle(String caseTitle) { this.caseTitle = caseTitle; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getDurationMs() { return durationMs; }
    public void setDurationMs(Integer durationMs) { this.durationMs = durationMs; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Instant getCreatedAt() { return createdAt; }
}
