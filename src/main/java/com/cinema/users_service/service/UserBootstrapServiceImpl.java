package com.cinema.users_service.service;

import com.cinema.users_service.constants.UsersConstants;
import com.cinema.users_service.domain.UserProfile;
import com.cinema.users_service.domain.Wallet;
import com.cinema.users_service.messaging.UserCreatedEvent;
import com.cinema.users_service.repository.UserProfileRepository;
import com.cinema.users_service.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserBootstrapServiceImpl implements UserBootstrapService {

    private final UserProfileRepository userProfileRepository;
    private final WalletRepository walletRepository;

    @Override
    @Transactional
    public void createFromEvent(UserCreatedEvent event) {
        Optional<UUID> userId = parseUserId(event.id());
        if (userId.isEmpty() || userProfileRepository.existsById(userId.get())) {
            return;
        }
        userProfileRepository.save(buildProfile(userId.get(), event));
        if (!walletRepository.existsByUserId(userId.get())) {
            walletRepository.save(buildWallet(userId.get()));
        }
        log.info("Profile and wallet created for user {}", userId.get());
    }

    private Optional<UUID> parseUserId(String rawId) {
        try {
            return Optional.of(UUID.fromString(rawId));
        } catch (Exception exception) {
            log.warn("Skipping USER_CREATED with invalid user id: {}", rawId);
            return Optional.empty();
        }
    }

    private UserProfile buildProfile(UUID userId, UserCreatedEvent event) {
        return UserProfile.builder()
                .id(userId)
                .name(event.name())
                .phone(event.phone())
                .build();
    }

    private Wallet buildWallet(UUID userId) {
        return Wallet.builder()
                .userId(userId)
                .balance(UsersConstants.DEFAULT_WALLET_BALANCE)
                .build();
    }
}
