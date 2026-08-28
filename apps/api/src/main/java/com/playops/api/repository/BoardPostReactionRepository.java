package com.playops.api.repository;

import com.playops.api.entity.BoardPostReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BoardPostReactionRepository extends JpaRepository<BoardPostReaction, Long> {
    Optional<BoardPostReaction> findByPostIdAndUsername(Long postId, String username);
    long countByPostIdAndLikedTrue(Long postId);
    long countByPostIdAndRatingIsNotNull(Long postId);
    void deleteByPostId(Long postId);

    @Query("select avg(r.rating) from BoardPostReaction r where r.post.id = :postId and r.rating is not null")
    Double averageRatingByPostId(@Param("postId") Long postId);
}
