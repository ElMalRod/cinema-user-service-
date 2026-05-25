package com.cinema.users_service.service;

import com.cinema.users_service.exception.ForbiddenAccessException;
import com.cinema.users_service.exception.InvalidHeaderException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class HeaderAccessServiceTest {

    private static final String VALID_USER_ID = "00000000-0000-0000-0000-000000000801";
    private static final UUID VALID_USER_UUID = UUID.fromString(VALID_USER_ID);
    private static final String INVALID_USER_ID = "invalid-uuid";
    private static final String SYSTEM_ADMIN_ROLE = "SYSTEM_ADMIN";
    private static final String CLIENT_ROLE = "CLIENT";
    private static final String INTERNAL_HEADER_VALUE = "true";
    private static final String INVALID_INTERNAL_HEADER = "false";

    @Test
    void should_ParseUserId_When_HeaderContainsValidUuid() {
        // Arrange
        HeaderAccessService headerAccessService = new HeaderAccessService();

        // Act
        UUID parsedUserId = headerAccessService.parseUserId(VALID_USER_ID);

        // Assert
        assertEquals(VALID_USER_UUID, parsedUserId);
    }

    @Test
    void should_ThrowInvalidHeaderException_When_HeaderContainsInvalidUuid() {
        // Arrange
        HeaderAccessService headerAccessService = new HeaderAccessService();

        // Act
        InvalidHeaderException exception = assertThrows(
                InvalidHeaderException.class,
                () -> headerAccessService.parseUserId(INVALID_USER_ID)
        );

        // Assert
        assertEquals("Header X-User-Id invalido", exception.getMessage());
    }

    @Test
    void should_AllowSystemAdmin_When_RoleHeaderMatchesSystemAdmin() {
        // Arrange
        HeaderAccessService headerAccessService = new HeaderAccessService();

        // Act
        headerAccessService.requireSystemAdmin(SYSTEM_ADMIN_ROLE);

        // Assert
    }

    @Test
    void should_ThrowForbiddenAccessException_When_RoleHeaderIsNotSystemAdmin() {
        // Arrange
        HeaderAccessService headerAccessService = new HeaderAccessService();

        // Act
        ForbiddenAccessException exception = assertThrows(
                ForbiddenAccessException.class,
                () -> headerAccessService.requireSystemAdmin(CLIENT_ROLE)
        );

        // Assert
        assertEquals("Solo SYSTEM_ADMIN puede ejecutar esta accion", exception.getMessage());
    }

    @Test
    void should_AllowInternalService_When_HeaderIsValid() {
        // Arrange
        HeaderAccessService headerAccessService = new HeaderAccessService();

        // Act
        headerAccessService.requireInternalService(INTERNAL_HEADER_VALUE);

        // Assert
    }

    @Test
    void should_ThrowForbiddenAccessException_When_InternalHeaderIsInvalid() {
        // Arrange
        HeaderAccessService headerAccessService = new HeaderAccessService();

        // Act
        ForbiddenAccessException exception = assertThrows(
                ForbiddenAccessException.class,
                () -> headerAccessService.requireInternalService(INVALID_INTERNAL_HEADER)
        );

        // Assert
        assertEquals("Acceso interno requerido", exception.getMessage());
    }
}
