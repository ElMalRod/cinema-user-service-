package com.cinema.users_service.service;

import com.cinema.users_service.messaging.UserCreatedEvent;
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

    private final UserProfileService userProfileService;

    @Override
    @Transactional
    public void createFromEvent(UserCreatedEvent event) {
        Optional<UUID> userId = parseUserId(event.id());
        if (userId.isEmpty()) {
            return;
        }
        userProfileService.createProfileAndWallet(userId.get(), event.name(), event.phone());
        log.info("Profile and wallet created from {} for user {}", event.event(), userId.get());
    }

    private Optional<UUID> parseUserId(String rawId) {
        try {
            return Optional.of(UUID.fromString(rawId));
        } catch (Exception exception) {
            log.warn("Skipping {} with invalid user id: {}", "user-events", rawId);
            return Optional.empty();
        }
    }
}
