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

    private AdminUserServiceImpl adminUserService;

    @BeforeEach
    void setUp() {
        adminUserService = new AdminUserServiceImpl(
                authServiceClient,
                userProfileService,
                walletRepository,
                temporaryPasswordService,
                credentialsNotificationService,
                "http://localhost:4200/forgot-password"
        );
    }

    @Test
    void createClientShouldCallAuthCreateProfileAndSendEmail() {
        // Arrange
        UUID userId = UUID.randomUUID();
        AdminCreateUserRequest request = new AdminCreateUserRequest(" Ana ", "5555", " ANA@TEST.COM ", UserRole.CLIENT);
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
        verify(credentialsNotificationService).sendWelcomeCredentials(" Ana ", " ANA@TEST.COM ", "TempPass123@", "http://localhost:4200/forgot-password");
        assertEquals(userId, response.userId());
        assertEquals("CLIENT", response.role());
    }

    @Test
    void createAdvertiserShouldCallAuthCreateProfileAndSendEmail() {
        // Arrange
        UUID userId = UUID.randomUUID();
        AdminCreateUserRequest request = new AdminCreateUserRequest("Acme", "4444", "ads@test.com", UserRole.ADVERTISER);
        when(temporaryPasswordService.generate()).thenReturn("TempPass123@");
        when(authServiceClient.createUser(any(AuthCreateUserRequest.class)))
                .thenReturn(new AuthCreateUserResponse(userId, "ads@test.com", "ADVERTISER", true, true));

        // Act
        adminUserService.createUser(request);

        // Assert
        verify(userProfileService).createProfileAndWallet(userId, "Acme", "4444");
        verify(credentialsNotificationService).sendWelcomeCredentials("Acme", "ads@test.com", "TempPass123@", "http://localhost:4200/forgot-password");
    }

    @Test
    void createWithDuplicatedEmailShouldThrowException() {
        // Arrange
        AdminCreateUserRequest request = new AdminCreateUserRequest("Ana", "5555", "ana@test.com", UserRole.CLIENT);
        when(temporaryPasswordService.generate()).thenReturn("TempPass123@");
        when(authServiceClient.createUser(any(AuthCreateUserRequest.class)))
                .thenThrow(new DuplicateUserException("El email ya existe en auth-service"));

        // Act
        DuplicateUserException exception = assertThrows(DuplicateUserException.class,
                () -> adminUserService.createUser(request));

        // Assert
        assertEquals("El email ya existe en auth-service", exception.getMessage());
        verify(userProfileService, never()).createProfileAndWallet(any(UUID.class), any(String.class), any(String.class));
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
