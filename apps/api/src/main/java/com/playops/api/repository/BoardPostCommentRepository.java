package com.playops.api.repository;

import com.playops.api.entity.BoardPostComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoardPostCommentRepository extends JpaRepository<BoardPostComment, Long> {
    List<BoardPostComment> findByPostIdOrderByCreatedAtAsc(Long postId);
    long countByPostId(Long postId);
    void deleteByPostId(Long postId);
}
