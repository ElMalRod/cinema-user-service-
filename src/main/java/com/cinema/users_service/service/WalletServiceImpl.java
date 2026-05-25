package com.cinema.users_service.service;

import com.cinema.users_service.constants.UsersConstants;
import com.cinema.users_service.domain.Wallet;
import com.cinema.users_service.domain.WalletTransaction;
import com.cinema.users_service.domain.WalletTransactionType;
import com.cinema.users_service.dto.wallet.WalletResponse;
import com.cinema.users_service.dto.wallet.WalletTransactionResponse;
import com.cinema.users_service.exception.InsufficientBalanceException;
import com.cinema.users_service.exception.InvalidAmountException;
import com.cinema.users_service.exception.UserProfileNotFoundException;
import com.cinema.users_service.repository.UserProfileRepository;
import com.cinema.users_service.repository.WalletRepository;
import com.cinema.users_service.repository.WalletTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class WalletServiceImpl implements WalletService {

    private final UserProfileRepository userProfileRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    public WalletServiceImpl(UserProfileRepository userProfileRepository,
                             WalletRepository walletRepository,
                             WalletTransactionRepository walletTransactionRepository) {
        this.userProfileRepository = userProfileRepository;
        this.walletRepository = walletRepository;
        this.walletTransactionRepository = walletTransactionRepository;
    }

    @Override
    public WalletResponse getWallet(UUID userId) {
        Wallet wallet = getWalletByUserId(userId);
        List<WalletTransactionResponse> transactions = walletTransactionRepository
                .findTop10ByWalletIdOrderByTransactionDateDesc(wallet.getId())
                .stream()
                .map(this::mapTransaction)
                .toList();
        return new WalletResponse(wallet.getBalance(), transactions);
    }

    @Override
    @Transactional
    public WalletResponse recharge(UUID userId, BigDecimal amount) {
        validateAmount(amount);
        Wallet wallet = getWalletByUserId(userId);
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);
        saveTransaction(wallet.getId(), amount, WalletTransactionType.RECHARGE, UsersConstants.RECHARGE_DESCRIPTION);
        return getWallet(userId);
    }

    @Override
    @Transactional
    public void deduct(UUID userId, BigDecimal amount, String description) {
        validateAmount(amount);
        Wallet wallet = getWalletByUserId(userId);
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Saldo insuficiente");
        }
        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);
        saveTransaction(wallet.getId(), amount, WalletTransactionType.PAYMENT, description);
    }

    private Wallet getWalletByUserId(UUID userId) {
        if (!userProfileRepository.existsById(userId)) {
            throw new UserProfileNotFoundException("Usuario no encontrado");
        }
        return walletRepository.findByUserId(userId)
                .orElseGet(() -> walletRepository.save(Wallet.builder()
                        .userId(userId)
                        .balance(UsersConstants.DEFAULT_WALLET_BALANCE)
                        .build()));
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("El monto debe ser mayor a cero");
        }
    }

    private void saveTransaction(UUID walletId, BigDecimal amount, WalletTransactionType type, String description) {
        WalletTransaction transaction = WalletTransaction.builder()
                .walletId(walletId)
                .amount(amount)
                .type(type)
                .description(description)
                .transactionDate(LocalDateTime.now())
                .build();
        walletTransactionRepository.save(transaction);
    }

    private WalletTransactionResponse mapTransaction(WalletTransaction transaction) {
        return new WalletTransactionResponse(
                transaction.getAmount(),
                transaction.getType().name(),
                transaction.getDescription(),
                transaction.getTransactionDate()
        );
    }
}