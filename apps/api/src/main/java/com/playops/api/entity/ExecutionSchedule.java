package com.playops.api.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * 프로젝트별 반복 예약 실행. 스케줄러(ExecutionScheduleService의 @Scheduled 폴러)가 매분 "지금이 실행 시각인가"만
 * 판단해서 기존 실행 파이프라인(TestExecutionService)을 그대로 트리거한다 — 스케줄러 자체는 docker.sock도,
 * 코드 접근 권한도 필요 없는 훨씬 작은 신뢰 경계다 (docs/ai-integration-architecture.md의 결정과 동일 원칙).
 */
@Entity
@Table(name = "execution_schedules")
public class ExecutionSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false, length = 100)
    private String projectId;

    @Column(length = 200)
    private String name;

    /** null/blank이면 전체 실행(등록된 spec 전체) */
    @Column(name = "spec_path", length = 500)
    private String specPath;

    @Column(nullable = false)
    private Integer hour;

    @Column(nullable = false)
    private Integer minute;

    /** "*"(매일) 또는 "MON,TUE,..." 형태의 3글자 요일 코드 CSV */
    @Column(name = "days_of_week", nullable = false, length = 50)
    private String daysOfWeek = "*";

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "last_triggered_at")
    private Instant lastTriggeredAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSpecPath() { return specPath; }
    public void setSpecPath(String specPath) { this.specPath = specPath; }

    public Integer getHour() { return hour; }
    public void setHour(Integer hour) { this.hour = hour; }

    public Integer getMinute() { return minute; }
    public void setMinute(Integer minute) { this.minute = minute; }

    public String getDaysOfWeek() { return daysOfWeek; }
    public void setDaysOfWeek(String daysOfWeek) { this.daysOfWeek = daysOfWeek; }

    public Boolean getEnabled() { return Boolean.TRUE.equals(enabled); }
    public void setEnabled(Boolean enabled) { this.enabled = Boolean.TRUE.equals(enabled); }

    public Instant getLastTriggeredAt() { return lastTriggeredAt; }
    public void setLastTriggeredAt(Instant lastTriggeredAt) { this.lastTriggeredAt = lastTriggeredAt; }

    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
