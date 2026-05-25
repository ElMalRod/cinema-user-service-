package com.cinema.users_service.dto.auth;

import com.cinema.users_service.domain.UserRole;

public record AuthCreateUserRequest(
        String name,
        String phone,
        String companyName,
        String email,
        String password,
        UserRole role,
        boolean forcePasswordChange
) {
    public AuthCreateUserRequest(String email, String password, UserRole role, boolean forcePasswordChange) {
        this("Usuario", null, null, email, password, role, forcePasswordChange);
    }
}
