package com.playops.api.dto;

import com.playops.api.entity.BoardPostType;
import java.time.Instant;
import java.util.List;

public record BoardPostRequest(
        BoardPostType type,
        String title,
        Long parentId,
        String content,
        List<BoardAttachment> attachments,
        Boolean visible,
        Boolean pinned,
        Instant visibleFrom,
        Instant visibleUntil
) {}
