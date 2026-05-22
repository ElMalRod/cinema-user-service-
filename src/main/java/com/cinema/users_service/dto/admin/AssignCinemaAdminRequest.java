package com.cinema.users_service.dto.admin;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignCinemaAdminRequest(
        @NotNull UUID cinemaId
) {
}
