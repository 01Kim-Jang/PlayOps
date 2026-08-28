package com.playops.api.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
        name = "board_post_reactions",
        uniqueConstraints = @UniqueConstraint(name = "uk_board_post_reaction_user", columnNames = {"post_id", "username"})
)
public class BoardPostReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private BoardPost post;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(nullable = false)
    private Boolean liked = false;

    @Column
    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String memo;

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

    public BoardPost getPost() { return post; }
    public void setPost(BoardPost post) { this.post = post; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public Boolean getLiked() { return Boolean.TRUE.equals(liked); }
    public void setLiked(Boolean liked) { this.liked = Boolean.TRUE.equals(liked); }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }
}
