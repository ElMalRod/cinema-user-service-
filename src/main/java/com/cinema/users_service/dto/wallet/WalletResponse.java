package com.cinema.users_service.dto.wallet;

import java.math.BigDecimal;
import java.util.List;

public record WalletResponse(
        BigDecimal balance,
        List<WalletTransactionResponse> transactions
) {
}