package com.cinema.users_service.messaging;

import com.cinema.users_service.service.UserBootstrapService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserEventsConsumerTest {

    @Mock
    private UserBootstrapService userBootstrapService;

    @InjectMocks
    private UserEventsConsumer consumer;

    @Test
    void shouldDelegateWhenEventIsUserCreated() {
        // Arrange
        UserCreatedEvent event = new UserCreatedEvent("USER_CREATED", "550e8400-e29b-41d4-a716-446655440000", "Ana", "5555");

        // Act
        consumer.onMessage(event);

        // Assert
        verify(userBootstrapService, times(1)).createFromEvent(event);
    }

    @Test
    void shouldIgnoreWhenEventIsDifferent() {
        // Arrange
        UserCreatedEvent event = new UserCreatedEvent("OTHER_EVENT", "550e8400-e29b-41d4-a716-446655440000", "Ana", "5555");

        // Act
        consumer.onMessage(event);

        // Assert
        verify(userBootstrapService, never()).createFromEvent(event);
    }
}
