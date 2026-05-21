package com.cinema.users_service.dto.wallet;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record WalletRechargeRequest(
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount
) {
}