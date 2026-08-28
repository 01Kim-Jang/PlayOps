package com.playops.api.dto;

import com.playops.api.entity.UserRole;

public record UserRequest(String username, String password, UserRole role) {}
