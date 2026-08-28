package com.playops.api.dto;

import java.util.List;

public record BoardPostCommentRequest(
        Long parentId,
        String content,
        List<BoardAttachment> attachments
) {}
