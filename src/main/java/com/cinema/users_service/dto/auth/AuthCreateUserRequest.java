package com.cinema.users_service.dto.auth;

import com.cinema.users_service.domain.UserRole;

public record AuthCreateUserRequest(
        String email,
        String password,
        UserRole role,
        boolean forcePasswordChange
) {
}