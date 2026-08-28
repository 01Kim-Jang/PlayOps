package com.playops.api.dto;

import com.playops.api.entity.UserRole;

public record LoginResponse(String token, String username, UserRole role) {}
