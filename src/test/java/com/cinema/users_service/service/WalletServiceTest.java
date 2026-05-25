package com.cinema.users_service.service;

import com.cinema.users_service.domain.UserProfile;
import com.cinema.users_service.domain.Wallet;
import com.cinema.users_service.domain.WalletTransaction;
import com.cinema.users_service.domain.WalletTransactionType;
import com.cinema.users_service.exception.InsufficientBalanceException;
import com.cinema.users_service.exception.InvalidAmountException;
import com.cinema.users_service.exception.UserProfileNotFoundException;
import com.cinema.users_service.repository.UserProfileRepository;
import com.cinema.users_service.repository.WalletRepository;
import com.cinema.users_service.repository.WalletTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletTransactionRepository walletTransactionRepository;

    @InjectMocks
    private WalletServiceImpl walletService;

    private UUID userId;
    private UUID walletId;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        walletId = UUID.randomUUID();
        wallet = Wallet.builder().id(walletId).userId(userId).balance(new BigDecimal("10.00")).build();
    }

    @Test
    void rechargeWalletShouldIncreaseBalance() {
        // Arrange
        when(userProfileRepository.existsById(userId)).thenReturn(true);
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(walletTransactionRepository.findTop10ByWalletIdOrderByTransactionDateDesc(walletId)).thenReturn(List.of());

        // Act
        walletService.recharge(userId, new BigDecimal("5.00"));

        // Assert
        assertEquals(new BigDecimal("15.00"), wallet.getBalance());
        verify(walletRepository).save(wallet);
        verify(walletTransactionRepository).save(any(WalletTransaction.class));
    }

    @Test
    void rechargeNegativeAmountShouldThrowException() {
        // Arrange
        BigDecimal invalidAmount = new BigDecimal("-1.00");

        // Act
        InvalidAmountException exception = assertThrows(InvalidAmountException.class,
                () -> walletService.recharge(userId, invalidAmount));

        // Assert
        assertEquals("El monto debe ser mayor a cero", exception.getMessage());
    }

    @Test
    void deductWithSufficientBalanceShouldDecreaseBalance() {
        // Arrange
        when(userProfileRepository.existsById(userId)).thenReturn(true);
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

        // Act
        walletService.deduct(userId, new BigDecimal("3.00"), "ticket payment");

        // Assert
        assertEquals(new BigDecimal("7.00"), wallet.getBalance());
        ArgumentCaptor<WalletTransaction> captor = ArgumentCaptor.forClass(WalletTransaction.class);
        verify(walletTransactionRepository).save(captor.capture());
        assertEquals(WalletTransactionType.PAYMENT, captor.getValue().getType());
    }

    @Test
    void deductWithInsufficientBalanceShouldThrowException() {
        // Arrange
        when(userProfileRepository.existsById(userId)).thenReturn(true);
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

        // Act
        InsufficientBalanceException exception = assertThrows(InsufficientBalanceException.class,
                () -> walletService.deduct(userId, new BigDecimal("99.00"), "payment"));

        // Assert
        assertEquals("Saldo insuficiente", exception.getMessage());
        verify(walletTransactionRepository, never()).save(any());
    }

    @Test
    void deductWithUnknownUserShouldThrowException() {
        // Arrange
        when(userProfileRepository.existsById(userId)).thenReturn(false);

        // Act
        UserProfileNotFoundException exception = assertThrows(UserProfileNotFoundException.class,
                () -> walletService.deduct(userId, new BigDecimal("1.00"), "payment"));

        // Assert
        assertEquals("Usuario no encontrado", exception.getMessage());
    }

    @Test
    void operationsShouldPersistWalletTransaction() {
        // Arrange
        when(userProfileRepository.existsById(userId)).thenReturn(true);
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(walletTransactionRepository.findTop10ByWalletIdOrderByTransactionDateDesc(walletId))
                .thenReturn(List.of(WalletTransaction.builder()
                        .walletId(walletId)
                        .amount(new BigDecimal("2.00"))
                        .type(WalletTransactionType.RECHARGE)
                        .description("Recarga")
                        .transactionDate(LocalDateTime.now())
                        .build()));

        // Act
        walletService.recharge(userId, new BigDecimal("2.00"));

        // Assert
        verify(walletTransactionRepository).save(any(WalletTransaction.class));
    }
}