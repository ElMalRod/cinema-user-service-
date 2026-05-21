package com.cinema.users_service.dto.profile;

import java.util.UUID;

public record ProfileResponse(
        UUID userId,
        String name,
        String phone,
        String email,
        String role
) {
}