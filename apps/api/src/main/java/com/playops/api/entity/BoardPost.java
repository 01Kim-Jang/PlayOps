package com.playops.api.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "board_posts")
public class BoardPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BoardPostType type = BoardPostType.FREE;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "attachments_json", columnDefinition = "TEXT")
    private String attachmentsJson;

    @Column(nullable = false)
    private Boolean visible = true;

    @Column(nullable = false)
    private Boolean pinned = false;

    @Column(name = "visible_from")
    private Instant visibleFrom;

    @Column(name = "visible_until")
    private Instant visibleUntil;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

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
    public void setId(Long id) { this.id = id; }

    public BoardPostType getType() { return type; }
    public void setType(BoardPostType type) { this.type = type != null ? type : BoardPostType.FREE; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getAttachmentsJson() { return attachmentsJson; }
    public void setAttachmentsJson(String attachmentsJson) { this.attachmentsJson = attachmentsJson; }

    public Boolean getVisible() { return Boolean.TRUE.equals(visible); }
    public void setVisible(Boolean visible) { this.visible = Boolean.TRUE.equals(visible); }

    public Boolean getPinned() { return Boolean.TRUE.equals(pinned); }
    public void setPinned(Boolean pinned) { this.pinned = Boolean.TRUE.equals(pinned); }

    public Instant getVisibleFrom() { return visibleFrom; }
    public void setVisibleFrom(Instant visibleFrom) { this.visibleFrom = visibleFrom; }

    public Instant getVisibleUntil() { return visibleUntil; }
    public void setVisibleUntil(Instant visibleUntil) { this.visibleUntil = visibleUntil; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
