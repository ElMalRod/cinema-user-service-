package com.cinema.users_service.dto.admin;

import com.cinema.users_service.domain.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AdminCreateUserRequest(
        @NotBlank String name,
        String phone,
        @NotBlank @Email String email,
        @NotNull UserRole role,
        String companyName,
        UUID cinemaId
) {
}
