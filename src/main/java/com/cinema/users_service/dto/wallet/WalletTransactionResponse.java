package com.cinema.users_service.dto.wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WalletTransactionResponse(
        BigDecimal amount,
        String type,
        String description,
        LocalDateTime transactionDate
) {
}