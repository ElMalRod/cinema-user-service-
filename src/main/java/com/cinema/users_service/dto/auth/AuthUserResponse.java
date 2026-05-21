package com.cinema.users_service.dto.auth;

import java.util.UUID;

public record AuthUserResponse(
        UUID id,
        String email,
        String role,
        boolean active
) {
}