package com.cinema.users_service.service;

import com.cinema.users_service.messaging.UserCreatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserBootstrapServiceImplTest {

    @Mock
    private UserProfileService userProfileService;

    @InjectMocks
    private UserBootstrapServiceImpl service;

    @Test
    void shouldCreateProfileAndWalletFromValidEvent() {
        // Arrange
        String userId = UUID.randomUUID().toString();
        UserCreatedEvent event = new UserCreatedEvent("USER_CREATED", userId, "Ana", "5555");

        // Act
        service.createFromEvent(event);

        // Assert
        verify(userProfileService).createProfileAndWallet(eq(UUID.fromString(userId)), eq("Ana"), eq("5555"));
    }

    @Test
    void shouldSkipEventWhenUserIdIsInvalid() {
        // Arrange
        UserCreatedEvent event = new UserCreatedEvent("USER_CREATED", "bad-id", "Ana", "5555");

        // Act
        service.createFromEvent(event);

        // Assert
        verify(userProfileService, never()).createProfileAndWallet(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}