package com.cinema.users_service.controller;

import com.cinema.users_service.domain.UserProfile;
import com.cinema.users_service.exception.UserProfileNotFoundException;
import com.cinema.users_service.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalUserControllerTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000903");
    private static final String USER_NAME = "Internal User";
    private static final String USER_NOT_FOUND_PREFIX = "Usuario no encontrado: ";

    @Mock
    private UserProfileRepository userProfileRepository;

    private InternalUserController internalUserController;

    @BeforeEach
    void setUp() {
        internalUserController = new InternalUserController(userProfileRepository);
    }

    @Test
    void should_ReturnUserSummary_When_UserExists() {
        // Arrange
        UserProfile userProfile = UserProfile.builder().id(USER_ID).name(USER_NAME).build();
        when(userProfileRepository.findById(USER_ID)).thenReturn(Optional.of(userProfile));

        // Act
        var result = internalUserController.getUserSummary(USER_ID);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(USER_ID, result.getBody().userId());
        assertEquals(USER_NAME, result.getBody().name());
    }

    @Test
    void should_ThrowUserProfileNotFoundException_When_UserDoesNotExist() {
        // Arrange
        when(userProfileRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // Act
        UserProfileNotFoundException exception = assertThrows(
                UserProfileNotFoundException.class,
                () -> internalUserController.getUserSummary(USER_ID)
        );

        // Assert
        assertEquals(USER_NOT_FOUND_PREFIX + USER_ID, exception.getMessage());
    }
}
