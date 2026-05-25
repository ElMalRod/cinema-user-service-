package com.cinema.users_service.controller;

import com.cinema.users_service.domain.UserRole;
import com.cinema.users_service.dto.admin.AdminCreateUserRequest;
import com.cinema.users_service.dto.admin.AdminUserResponse;
import com.cinema.users_service.dto.admin.AssignCinemaAdminRequest;
import com.cinema.users_service.service.AdminUserService;
import com.cinema.users_service.service.HeaderAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    private static final String SYSTEM_ADMIN_ROLE = "SYSTEM_ADMIN";
    private static final String USER_NAME = "Admin Name";
    private static final String USER_PHONE = "5551000";
    private static final String USER_EMAIL = "admin@test.com";
    private static final String ROLE_CLIENT = "CLIENT";
    private static final boolean ACTIVE_USER = true;
    private static final BigDecimal BALANCE = BigDecimal.ZERO;
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000901");
    private static final UUID CINEMA_ID = UUID.fromString("00000000-0000-0000-0000-000000000902");

    @Mock
    private AdminUserService adminUserService;

    @Mock
    private HeaderAccessService headerAccessService;

    private AdminUserController adminUserController;

    @BeforeEach
    void setUp() {
        adminUserController = new AdminUserController(adminUserService, headerAccessService);
    }

    @Test
    void should_CreateUser_When_RequestIsValid() {
        // Arrange
        AdminCreateUserRequest request = new AdminCreateUserRequest(USER_NAME, USER_PHONE, USER_EMAIL, UserRole.CLIENT, null, null);
        AdminUserResponse response = buildAdminUserResponse();
        when(adminUserService.createUser(request)).thenReturn(response);

        // Act
        var result = adminUserController.createUser(SYSTEM_ADMIN_ROLE, request);

        // Assert
        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(USER_ID, result.getBody().userId());
        verify(headerAccessService).requireSystemAdmin(SYSTEM_ADMIN_ROLE);
    }

    @Test
    void should_ListUsers_When_RequestIsValid() {
        // Arrange
        when(adminUserService.listUsers()).thenReturn(List.of(buildAdminUserResponse()));

        // Act
        var result = adminUserController.listUsers(SYSTEM_ADMIN_ROLE);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().size());
        verify(headerAccessService).requireSystemAdmin(SYSTEM_ADMIN_ROLE);
    }

    @Test
    void should_ListUnassignedCinemaAdmins_When_RequestIsValid() {
        // Arrange
        when(adminUserService.listUnassignedCinemaAdmins()).thenReturn(List.of(buildAdminUserResponse()));

        // Act
        var result = adminUserController.listUnassignedCinemaAdmins(SYSTEM_ADMIN_ROLE);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().size());
        verify(headerAccessService).requireSystemAdmin(SYSTEM_ADMIN_ROLE);
    }

    @Test
    void should_AssignCinemaAdmin_When_RequestIsValid() {
        // Arrange
        AssignCinemaAdminRequest request = new AssignCinemaAdminRequest(CINEMA_ID);

        // Act
        var result = adminUserController.assignCinemaAdmin(SYSTEM_ADMIN_ROLE, USER_ID, request);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        verify(adminUserService).assignCinemaAdmin(USER_ID, CINEMA_ID);
    }

    @Test
    void should_DeactivateUser_When_RequestIsValid() {
        // Arrange

        // Act
        var result = adminUserController.deactivateUser(SYSTEM_ADMIN_ROLE, USER_ID);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        verify(adminUserService).deactivateUser(USER_ID);
    }

    @Test
    void should_ActivateUser_When_RequestIsValid() {
        // Arrange

        // Act
        var result = adminUserController.activateUser(SYSTEM_ADMIN_ROLE, USER_ID);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        verify(adminUserService).activateUser(USER_ID);
    }

    private AdminUserResponse buildAdminUserResponse() {
        return new AdminUserResponse(USER_ID, USER_NAME, USER_PHONE, USER_EMAIL, ROLE_CLIENT, ACTIVE_USER, BALANCE);
    }
}
