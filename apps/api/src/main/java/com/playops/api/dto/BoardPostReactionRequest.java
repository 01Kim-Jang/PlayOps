package com.playops.api.dto;

public record BoardPostReactionRequest(
        Boolean liked,
        Integer rating,
        String memo
) {}
