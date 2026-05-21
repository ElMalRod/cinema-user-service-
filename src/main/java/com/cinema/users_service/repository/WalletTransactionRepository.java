package com.cinema.users_service.repository;

import com.cinema.users_service.domain.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, UUID> {

    List<WalletTransaction> findTop10ByWalletIdOrderByTransactionDateDesc(UUID walletId);
}