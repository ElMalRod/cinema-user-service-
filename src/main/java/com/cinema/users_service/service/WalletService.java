package com.cinema.users_service.service;

import com.cinema.users_service.dto.wallet.WalletResponse;

import java.math.BigDecimal;
import java.util.UUID;

public interface WalletService {

    WalletResponse getWallet(UUID userId);

    WalletResponse recharge(UUID userId, BigDecimal amount);

    void deduct(UUID userId, BigDecimal amount, String description);
}