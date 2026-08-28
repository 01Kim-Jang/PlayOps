package com.playops.api.dto;

import com.playops.api.entity.BoardPost;
import com.playops.api.entity.BoardPostType;
import java.time.Instant;
import java.util.List;

public record BoardPostResponse(
        Long id,
        BoardPostType type,
        String title,
        Long parentId,
        String content,
        List<BoardAttachment> attachments,
        Boolean visible,
        Boolean pinned,
        Instant visibleFrom,
        Instant visibleUntil,
        String createdBy,
        String updatedBy,
        Instant createdAt,
        Instant updatedAt,
        Long likeCount,
        Double ratingAverage,
        Long ratingCount,
        Long commentCount,
        Long replyCount,
        Boolean myLiked,
        Integer myRating,
        String myMemo
) {
    public static BoardPostResponse from(
            BoardPost post,
            Long likeCount,
            Double ratingAverage,
            Long ratingCount,
            Long commentCount,
            Long replyCount,
            Boolean myLiked,
            Integer myRating,
            String myMemo,
            List<BoardAttachment> attachments
    ) {
        return new BoardPostResponse(
                post.getId(),
                post.getType(),
                post.getTitle(),
                post.getParentId(),
                post.getContent(),
                attachments,
                post.getVisible(),
                post.getPinned(),
                post.getVisibleFrom(),
                post.getVisibleUntil(),
                post.getCreatedBy(),
                post.getUpdatedBy(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                likeCount != null ? likeCount : 0,
                ratingAverage,
                ratingCount != null ? ratingCount : 0,
                commentCount != null ? commentCount : 0,
                replyCount != null ? replyCount : 0,
                Boolean.TRUE.equals(myLiked),
                myRating,
                myMemo
        );
    }
}
