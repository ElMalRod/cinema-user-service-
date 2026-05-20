package com.cinema.users_service.service;

import com.cinema.users_service.messaging.UserCreatedEvent;
import com.cinema.users_service.repository.UserProfileRepository;
import com.cinema.users_service.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserBootstrapServiceImplTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private UserBootstrapServiceImpl service;

    private String userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID().toString();
    }

    @Test
    void shouldCreateProfileAndWalletWhenUserDoesNotExist() {
        // Arrange
        UserCreatedEvent event = new UserCreatedEvent("USER_CREATED", userId, "Ana", "5555");
        when(userProfileRepository.existsById(any())).thenReturn(false);
        when(walletRepository.existsByUserId(any())).thenReturn(false);

        // Act
        service.createFromEvent(event);

        // Assert
        verify(userProfileRepository, times(1)).save(any());
        verify(walletRepository, times(1)).save(any());
    }

    @Test
    void shouldSkipWhenProfileAlreadyExists() {
        // Arrange
        UserCreatedEvent event = new UserCreatedEvent("USER_CREATED", userId, "Ana", "5555");
        when(userProfileRepository.existsById(any())).thenReturn(true);

        // Act
        service.createFromEvent(event);

        // Assert
        verify(userProfileRepository, never()).save(any());
        verify(walletRepository, never()).save(any());
    }

    @Test
    void shouldSkipWhenUserIdIsInvalid() {
        // Arrange
        UserCreatedEvent event = new UserCreatedEvent("USER_CREATED", "bad-id", "Ana", "5555");

        // Act
        service.createFromEvent(event);

        // Assert
        verify(userProfileRepository, never()).save(any());
        verify(walletRepository, never()).save(any());
    }
}
