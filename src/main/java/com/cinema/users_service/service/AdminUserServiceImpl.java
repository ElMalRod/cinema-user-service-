package com.cinema.users_service.service;

import com.cinema.users_service.constants.UsersConstants;
import com.cinema.users_service.domain.UserProfile;
import com.cinema.users_service.dto.admin.AdminCreateUserRequest;
import com.cinema.users_service.dto.admin.AdminUserResponse;
import com.cinema.users_service.dto.auth.AuthCreateUserRequest;
import com.cinema.users_service.dto.auth.AuthCreateUserResponse;
import com.cinema.users_service.dto.auth.AuthUserResponse;
import com.cinema.users_service.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    private final AuthServiceClient authServiceClient;
    private final UserProfileService userProfileService;
    private final WalletRepository walletRepository;
    private final TemporaryPasswordService temporaryPasswordService;
    private final CredentialsNotificationService credentialsNotificationService;
    private final String passwordChangeUrl;

    public AdminUserServiceImpl(AuthServiceClient authServiceClient,
                                UserProfileService userProfileService,
                                WalletRepository walletRepository,
                                TemporaryPasswordService temporaryPasswordService,
                                CredentialsNotificationService credentialsNotificationService,
                                @Value("${users.password-change-url:http://localhost:4200/forgot-password}") String passwordChangeUrl) {
        this.authServiceClient = authServiceClient;
        this.userProfileService = userProfileService;
        this.walletRepository = walletRepository;
        this.temporaryPasswordService = temporaryPasswordService;
        this.credentialsNotificationService = credentialsNotificationService;
        this.passwordChangeUrl = passwordChangeUrl;
    }

    @Override
    public void ensureInitialSystemAdmin() {
        AuthUserResponse systemAdmin = resolveOrCreateDefaultSystemAdmin();
        userProfileService.createProfileAndWallet(systemAdmin.id(), UsersConstants.DEFAULT_SYSTEM_ADMIN_NAME, null);
    }

    @Override
    public AdminUserResponse createUser(AdminCreateUserRequest request) {
        String temporaryPassword = temporaryPasswordService.generate();
        AuthCreateUserResponse created = authServiceClient.createUser(buildAuthRequest(request, temporaryPassword));
        userProfileService.createProfileAndWallet(created.id(), request.name().trim(), request.phone());
        credentialsNotificationService.sendWelcomeCredentials(request.name(), request.email(), temporaryPassword, passwordChangeUrl);
        return mapCreatedUser(created, request);
    }

    @Override
    public List<AdminUserResponse> listUsers() {
        return authServiceClient.listUsers().stream().map(this::mapUser).toList();
    }

    @Override
    public void deactivateUser(UUID userId) {
        authServiceClient.deactivateUser(userId);
    }

    @Override
    public void activateUser(UUID userId) {
        authServiceClient.activateUser(userId);
    }

    private AuthUserResponse resolveOrCreateDefaultSystemAdmin() {
        AuthUserResponse existingDefault = findUserByEmail(UsersConstants.DEFAULT_SYSTEM_ADMIN_EMAIL);
        if (existingDefault != null && existingDefault.active()) {
            return existingDefault;
        }

        if (authServiceClient.existsSystemAdmin()) {
            AuthUserResponse reloadedDefault = findUserByEmail(UsersConstants.DEFAULT_SYSTEM_ADMIN_EMAIL);
            if (reloadedDefault != null) {
                return reloadedDefault;
            }
        }

        AuthCreateUserResponse created = authServiceClient.createUser(new AuthCreateUserRequest(
                UsersConstants.DEFAULT_SYSTEM_ADMIN_EMAIL,
                UsersConstants.DEFAULT_SYSTEM_ADMIN_PASSWORD,
                com.cinema.users_service.domain.UserRole.SYSTEM_ADMIN,
                false
        ));

        return new AuthUserResponse(created.id(), created.email(), created.role(), created.active());
    }

    private AuthUserResponse findUserByEmail(String email) {
        String normalized = normalizeEmail(email);
        return authServiceClient.listUsers().stream()
                .filter(user -> normalizeEmail(user.email()).equals(normalized))
                .findFirst()
                .orElse(null);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private AuthCreateUserRequest buildAuthRequest(AdminCreateUserRequest request, String temporaryPassword) {
        return new AuthCreateUserRequest(
                request.email().trim().toLowerCase(Locale.ROOT),
                temporaryPassword,
                request.role(),
                true
        );
    }

    private AdminUserResponse mapCreatedUser(AuthCreateUserResponse created, AdminCreateUserRequest request) {
        return new AdminUserResponse(
                created.id(),
                request.name(),
                request.phone(),
                created.email(),
                created.role(),
                created.active(),
                UsersConstants.DEFAULT_WALLET_BALANCE
        );
    }

    private AdminUserResponse mapUser(AuthUserResponse authUser) {
        UserProfile profile = userProfileService.findProfile(authUser.id()).orElse(null);
        java.math.BigDecimal balance = walletRepository.findByUserId(authUser.id())
                .map(wallet -> wallet.getBalance())
                .orElse(UsersConstants.DEFAULT_WALLET_BALANCE);
        String name = profile == null ? null : profile.getName();
        String phone = profile == null ? null : profile.getPhone();
        return new AdminUserResponse(authUser.id(), name, phone, authUser.email(), authUser.role(), authUser.active(), balance);
    }
}
