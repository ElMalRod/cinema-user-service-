package com.cinema.users_service.dto.profile;

import jakarta.validation.constraints.NotBlank;

public record UpdateProfileRequest(
        @NotBlank String name,
        String phone
) {
}