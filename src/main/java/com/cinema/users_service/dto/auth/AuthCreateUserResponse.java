package com.cinema.users_service.dto.auth;

import java.util.UUID;

public record AuthCreateUserResponse(
        UUID id,
        String email,
        String role,
        boolean active,
        boolean requiresPasswordChange
) {
}