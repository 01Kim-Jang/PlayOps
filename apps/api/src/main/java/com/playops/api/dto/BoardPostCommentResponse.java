package com.playops.api.dto;

import com.playops.api.entity.BoardPostComment;
import java.time.Instant;
import java.util.List;

public record BoardPostCommentResponse(
        Long id,
        Long parentId,
        String content,
        List<BoardAttachment> attachments,
        String createdBy,
        String updatedBy,
        Instant createdAt,
        Instant updatedAt
) {
    public static BoardPostCommentResponse from(BoardPostComment comment, List<BoardAttachment> attachments) {
        return new BoardPostCommentResponse(
                comment.getId(),
                comment.getParentId(),
                comment.getContent(),
                attachments,
                comment.getCreatedBy(),
                comment.getUpdatedBy(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}
