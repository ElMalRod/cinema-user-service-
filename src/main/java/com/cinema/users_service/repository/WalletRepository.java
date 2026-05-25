package com.cinema.users_service.repository;

import com.cinema.users_service.domain.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    boolean existsByUserId(UUID userId);

    Optional<Wallet> findByUserId(UUID userId);
}