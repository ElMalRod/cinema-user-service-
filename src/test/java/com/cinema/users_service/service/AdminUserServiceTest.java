package com.cinema.users_service.service;

import com.cinema.users_service.constants.UsersConstants;
import com.cinema.users_service.domain.UserProfile;
import com.cinema.users_service.domain.UserRole;
import com.cinema.users_service.domain.Wallet;
import com.cinema.users_service.dto.admin.AdminCreateUserRequest;
import com.cinema.users_service.dto.admin.AdminUserResponse;
import com.cinema.users_service.dto.auth.AuthCreateUserRequest;
import com.cinema.users_service.dto.auth.AuthCreateUserResponse;
import com.cinema.users_service.dto.auth.AuthUserResponse;
import com.cinema.users_service.exception.DuplicateUserException;
import com.cinema.users_service.exception.InvalidAdminOperationException;
import com.cinema.users_service.exception.UserProfileNotFoundException;
import com.cinema.users_service.messaging.UserCreatedEvent;
import com.cinema.users_service.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private AuthServiceClient authServiceClient;

    @Mock
    private UserProfileService userProfileService;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TemporaryPasswordService temporaryPasswordService;

    @Mock
    private CredentialsNotificationService credentialsNotificationService;

    @Mock
    private CinemaServiceClient cinemaServiceClient;

    @Mock
    private UserEventsPublisher userEventsPublisher;

    private AdminUserServiceImpl adminUserService;

    @BeforeEach
    void setUp() {
        adminUserService = new AdminUserServiceImpl(
                authServiceClient,
                userProfileService,
                walletRepository,
                temporaryPasswordService,
                credentialsNotificationService,
                cinemaServiceClient,
                userEventsPublisher,
                "http://localhost:4200/forgot-password"
        );
    }

    @Test
    void createClientShouldCallAuthCreateProfilePublishEventAndSendEmail() {
        // Arrange
        UUID userId = UUID.randomUUID();
        AdminCreateUserRequest request = new AdminCreateUserRequest(" Ana ", "5555", " ANA@TEST.COM ", UserRole.CLIENT, null, null);
        when(temporaryPasswordService.generate()).thenReturn("TempPass123@");
        when(authServiceClient.createUser(any(AuthCreateUserRequest.class)))
                .thenReturn(new AuthCreateUserResponse(userId, "ana@test.com", "CLIENT", true, true));

        // Act
        AdminUserResponse response = adminUserService.createUser(request);

        // Assert
        ArgumentCaptor<AuthCreateUserRequest> authRequestCaptor = ArgumentCaptor.forClass(AuthCreateUserRequest.class);
        verify(authServiceClient).createUser(authRequestCaptor.capture());
        AuthCreateUserRequest sentRequest = authRequestCaptor.getValue();
        assertEquals("ana@test.com", sentRequest.email());
        assertEquals("TempPass123@", sentRequest.password());
        assertEquals(UserRole.CLIENT, sentRequest.role());
        assertTrue(sentRequest.forcePasswordChange());

        verify(userProfileService).createProfileAndWallet(userId, "Ana", "5555");
        verify(cinemaServiceClient, never()).assignCinemaAdmin(any(UUID.class), any(UUID.class));

        ArgumentCaptor<UserCreatedEvent> eventCaptor = ArgumentCaptor.forClass(UserCreatedEvent.class);
        verify(userEventsPublisher).publish(eventCaptor.capture());
        UserCreatedEvent event = eventCaptor.getValue();
        assertEquals(UsersConstants.EVENT_USER_CREATED, event.event());
        assertEquals(userId.toString(), event.id());
        assertEquals("Ana", event.name());
        assertEquals("5555", event.phone());
        assertNull(event.companyName());

        verify(credentialsNotificationService).sendWelcomeCredentials(" Ana ", " ANA@TEST.COM ", "TempPass123@", "http://localhost:4200/forgot-password");
        assertEquals(userId, response.userId());
        assertEquals("CLIENT", response.role());
    }

    @Test
    void createAdvertiserShouldPublishAdvertiserEvent() {
        // Arrange
        UUID userId = UUID.randomUUID();
        AdminCreateUserRequest request = new AdminCreateUserRequest("Acme", "4444", "ads@test.com", UserRole.ADVERTISER, null, null);
        when(temporaryPasswordService.generate()).thenReturn("TempPass123@");
        when(authServiceClient.createUser(any(AuthCreateUserRequest.class)))
                .thenReturn(new AuthCreateUserResponse(userId, "ads@test.com", "ADVERTISER", true, true));

        // Act
        adminUserService.createUser(request);

        // Assert
        verify(userProfileService).createProfileAndWallet(userId, "Acme", "4444");
        verify(cinemaServiceClient, never()).assignCinemaAdmin(any(UUID.class), any(UUID.class));

        ArgumentCaptor<UserCreatedEvent> eventCaptor = ArgumentCaptor.forClass(UserCreatedEvent.class);
        verify(userEventsPublisher).publish(eventCaptor.capture());
        UserCreatedEvent event = eventCaptor.getValue();
        assertEquals(UsersConstants.EVENT_ADVERTISER_CREATED, event.event());
        assertEquals(userId.toString(), event.id());
        assertEquals("Acme", event.name());
        assertEquals("4444", event.phone());
        assertNull(event.companyName());

        verify(credentialsNotificationService).sendWelcomeCredentials("Acme", "ads@test.com", "TempPass123@", "http://localhost:4200/forgot-password");
    }

    @Test
    void createCinemaAdminWithCompanyNameShouldAssignCinemaAndPublishCinemaEvent() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID cinemaId = UUID.randomUUID();
        AdminCreateUserRequest request = new AdminCreateUserRequest(
                "Cinema Admin",
                "6666",
                "cinema@test.com",
                UserRole.CINEMA_ADMIN,
                "  Cinepolis Majadas  ",
                cinemaId
        );
        when(temporaryPasswordService.generate()).thenReturn("TempPass123@");
        when(authServiceClient.createUser(any(AuthCreateUserRequest.class)))
                .thenReturn(new AuthCreateUserResponse(userId, "cinema@test.com", "CINEMA_ADMIN", true, true));

        // Act
        adminUserService.createUser(request);

        // Assert
        ArgumentCaptor<UserCreatedEvent> eventCaptor = ArgumentCaptor.forClass(UserCreatedEvent.class);
        verify(userEventsPublisher).publish(eventCaptor.capture());
        UserCreatedEvent event = eventCaptor.getValue();
        assertEquals(UsersConstants.EVENT_CINEMA_ADMIN_CREATED, event.event());
        assertEquals(userId.toString(), event.id());
        assertEquals("Cinema Admin", event.name());
        assertEquals("6666", event.phone());
        assertEquals("Cinepolis Majadas", event.companyName());

        verify(cinemaServiceClient).assignCinemaAdmin(cinemaId, userId);
    }

    @Test
    void createCinemaAdminWithoutCompanyNameShouldPublishNullCompanyName() {
        // Arrange
        UUID userId = UUID.randomUUID();
        AdminCreateUserRequest request = new AdminCreateUserRequest(
                "Cinema Admin",
                "6666",
                "cinema@test.com",
                UserRole.CINEMA_ADMIN,
                null,
                null
        );
        when(temporaryPasswordService.generate()).thenReturn("TempPass123@");
        when(authServiceClient.createUser(any(AuthCreateUserRequest.class)))
                .thenReturn(new AuthCreateUserResponse(userId, "cinema@test.com", "CINEMA_ADMIN", true, true));

        // Act
        adminUserService.createUser(request);

        // Assert
        ArgumentCaptor<UserCreatedEvent> eventCaptor = ArgumentCaptor.forClass(UserCreatedEvent.class);
        verify(userEventsPublisher).publish(eventCaptor.capture());
        UserCreatedEvent event = eventCaptor.getValue();
        assertEquals(UsersConstants.EVENT_CINEMA_ADMIN_CREATED, event.event());
        assertNull(event.companyName());
    }

    @Test
    void createWithDuplicatedEmailShouldThrowException() {
        // Arrange
        AdminCreateUserRequest request = new AdminCreateUserRequest("Ana", "5555", "ana@test.com", UserRole.CLIENT, null, null);
        when(temporaryPasswordService.generate()).thenReturn("TempPass123@");
        when(authServiceClient.createUser(any(AuthCreateUserRequest.class)))
                .thenThrow(new DuplicateUserException("El email ya existe en auth-service"));

        // Act
        DuplicateUserException exception = assertThrows(DuplicateUserException.class,
                () -> adminUserService.createUser(request));

        // Assert
        assertEquals("El email ya existe en auth-service", exception.getMessage());
        verify(userProfileService, never()).createProfileAndWallet(any(UUID.class), any(String.class), any(String.class));
        verify(userEventsPublisher, never()).publish(any(UserCreatedEvent.class));
    }

    @Test
    void listUnassignedCinemaAdminsShouldReturnOnlyPendingOnes() {
        // Arrange
        UUID assignedId = UUID.randomUUID();
        UUID pendingId = UUID.randomUUID();
        AuthUserResponse assigned = new AuthUserResponse(assignedId, "assigned@test.com", "CINEMA_ADMIN", true);
        AuthUserResponse pending = new AuthUserResponse(pendingId, "pending@test.com", "CINEMA_ADMIN", true);
        when(authServiceClient.listUsers()).thenReturn(List.of(assigned, pending));
        when(cinemaServiceClient.hasCinemaAssigned(assignedId)).thenReturn(true);
        when(cinemaServiceClient.hasCinemaAssigned(pendingId)).thenReturn(false);
        when(userProfileService.findProfile(pendingId)).thenReturn(Optional.empty());
        when(walletRepository.findByUserId(pendingId)).thenReturn(Optional.empty());

        // Act
        List<AdminUserResponse> result = adminUserService.listUnassignedCinemaAdmins();

        // Assert
        assertEquals(1, result.size());
        assertEquals(pendingId, result.getFirst().userId());
    }

    @Test
    void assignCinemaAdminShouldThrowWhenUserDoesNotExist() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID cinemaId = UUID.randomUUID();
        when(authServiceClient.findUserById(userId)).thenReturn(Optional.empty());

        // Act
        UserProfileNotFoundException exception = assertThrows(UserProfileNotFoundException.class,
                () -> adminUserService.assignCinemaAdmin(userId, cinemaId));

        // Assert
        assertEquals("Usuario no encontrado", exception.getMessage());
    }

    @Test
    void assignCinemaAdminShouldThrowWhenRoleIsInvalid() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID cinemaId = UUID.randomUUID();
        AuthUserResponse clientUser = new AuthUserResponse(userId, "client@test.com", "CLIENT", true);
        when(authServiceClient.findUserById(userId)).thenReturn(Optional.of(clientUser));

        // Act
        InvalidAdminOperationException exception = assertThrows(InvalidAdminOperationException.class,
                () -> adminUserService.assignCinemaAdmin(userId, cinemaId));

        // Assert
        assertEquals("Solo usuarios CINEMA_ADMIN activos se pueden asignar a un cine", exception.getMessage());
        verify(cinemaServiceClient, never()).assignCinemaAdmin(any(UUID.class), any(UUID.class));
    }

    @Test
    void ensureInitialSystemAdminWhenAlreadyExistsShouldEnsureProfileAndWallet() {
        // Arrange
        UUID adminId = UUID.randomUUID();
        AuthUserResponse existingAdmin = new AuthUserResponse(adminId, UsersConstants.DEFAULT_SYSTEM_ADMIN_EMAIL, UsersConstants.ROLE_SYSTEM_ADMIN, true);
        when(authServiceClient.listUsers()).thenReturn(List.of(existingAdmin));

        // Act
        adminUserService.ensureInitialSystemAdmin();

        // Assert
        verify(authServiceClient, never()).createUser(any(AuthCreateUserRequest.class));
        verify(userProfileService).createProfileAndWallet(adminId, UsersConstants.DEFAULT_SYSTEM_ADMIN_NAME, null);
    }

    @Test
    void ensureInitialSystemAdminWhenMissingShouldCreateIt() {
        // Arrange
        UUID adminId = UUID.randomUUID();
        when(authServiceClient.listUsers()).thenReturn(List.of());
        when(authServiceClient.existsSystemAdmin()).thenReturn(false);
        when(authServiceClient.createUser(any(AuthCreateUserRequest.class)))
                .thenReturn(new AuthCreateUserResponse(adminId, UsersConstants.DEFAULT_SYSTEM_ADMIN_EMAIL, "SYSTEM_ADMIN", true, false));

        // Act
        adminUserService.ensureInitialSystemAdmin();

        // Assert
        ArgumentCaptor<AuthCreateUserRequest> captor = ArgumentCaptor.forClass(AuthCreateUserRequest.class);
        verify(authServiceClient).createUser(captor.capture());
        AuthCreateUserRequest request = captor.getValue();
        assertEquals(UsersConstants.DEFAULT_SYSTEM_ADMIN_EMAIL, request.email());
        assertEquals(UsersConstants.DEFAULT_SYSTEM_ADMIN_PASSWORD, request.password());
        assertEquals(UserRole.SYSTEM_ADMIN, request.role());
        assertTrue(!request.forcePasswordChange());
        verify(userProfileService).createProfileAndWallet(adminId, UsersConstants.DEFAULT_SYSTEM_ADMIN_NAME, null);
    }

    @Test
    void listUsersShouldMapProfileAndWalletData() {
        // Arrange
        UUID userId = UUID.randomUUID();
        AuthUserResponse authUser = new AuthUserResponse(userId, "ana@test.com", "CLIENT", true);
        UserProfile profile = UserProfile.builder().id(userId).name("Ana").phone("5555").build();
        Wallet wallet = Wallet.builder().userId(userId).balance(new BigDecimal("25.00")).build();

        when(authServiceClient.listUsers()).thenReturn(List.of(authUser));
        when(userProfileService.findProfile(userId)).thenReturn(Optional.of(profile));
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

        // Act
        List<AdminUserResponse> users = adminUserService.listUsers();

        // Assert
        assertEquals(1, users.size());
        assertEquals("Ana", users.getFirst().name());
        assertEquals("5555", users.getFirst().phone());
        assertEquals(new BigDecimal("25.00"), users.getFirst().balance());
    }

    @Test
    void listUsersShouldFallbackWhenProfileOrWalletMissing() {
        // Arrange
        UUID userId = UUID.randomUUID();
        AuthUserResponse authUser = new AuthUserResponse(userId, "ads@test.com", "ADVERTISER", true);

        when(authServiceClient.listUsers()).thenReturn(List.of(authUser));
        when(userProfileService.findProfile(userId)).thenReturn(Optional.empty());
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // Act
        List<AdminUserResponse> users = adminUserService.listUsers();

        // Assert
        assertEquals(1, users.size());
        assertEquals(null, users.getFirst().name());
        assertEquals(UsersConstants.DEFAULT_WALLET_BALANCE, users.getFirst().balance());
    }

    @Test
    void deactivateUserShouldDelegateToAuthClient() {
        // Arrange
        UUID userId = UUID.randomUUID();

        // Act
        adminUserService.deactivateUser(userId);

        // Assert
        verify(authServiceClient).deactivateUser(userId);
    }

    @Test
    void activateUserShouldDelegateToAuthClient() {
        // Arrange
        UUID userId = UUID.randomUUID();

        // Act
        adminUserService.activateUser(userId);

        // Assert
        verify(authServiceClient).activateUser(userId);
    }
}
