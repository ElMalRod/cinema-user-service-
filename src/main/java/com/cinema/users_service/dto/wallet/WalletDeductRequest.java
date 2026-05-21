package com.cinema.users_service.dto.wallet;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletDeductRequest(
        @NotNull UUID userId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        String description
) {
}