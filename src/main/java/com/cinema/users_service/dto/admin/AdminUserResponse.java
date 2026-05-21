package com.cinema.users_service.dto.admin;

import java.math.BigDecimal;
import java.util.UUID;

public record AdminUserResponse(
        UUID userId,
        String name,
        String phone,
        String email,
        String role,
        boolean active,
        BigDecimal balance
) {
}