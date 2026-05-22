package com.cinema.users_service.dto.internal;

import java.util.UUID;

public record UserSummaryResponse(UUID userId, String name) {
}
