package com.cinema.users_service.service;

import com.cinema.users_service.constants.UsersConstants;
import com.cinema.users_service.domain.UserProfile;
import com.cinema.users_service.domain.UserRole;
import com.cinema.users_service.dto.admin.AdminCreateUserRequest;
import com.cinema.users_service.dto.admin.AdminUserResponse;
import com.cinema.users_service.dto.auth.AuthCreateUserRequest;
import com.cinema.users_service.dto.auth.AuthCreateUserResponse;
import com.cinema.users_service.dto.auth.AuthUserResponse;
import com.cinema.users_service.exception.InvalidAdminOperationException;
import com.cinema.users_service.exception.UserProfileNotFoundException;
import com.cinema.users_service.messaging.UserCreatedEvent;
import com.cinema.users_service.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    private static final Logger log = LoggerFactory.getLogger(AdminUserServiceImpl.class);

    private final AuthServiceClient authServiceClient;
    private final UserProfileService userProfileService;
    private final WalletRepository walletRepository;
    private final TemporaryPasswordService temporaryPasswordService;
    private final CredentialsNotificationService credentialsNotificationService;
    private final CinemaServiceClient cinemaServiceClient;
    private final UserEventsPublisher userEventsPublisher;
    private final String passwordChangeUrl;

    public AdminUserServiceImpl(AuthServiceClient authServiceClient,
                                UserProfileService userProfileService,
                                WalletRepository walletRepository,
                                TemporaryPasswordService temporaryPasswordService,
                                CredentialsNotificationService credentialsNotificationService,
                                CinemaServiceClient cinemaServiceClient,
                                UserEventsPublisher userEventsPublisher,
                                @Value("${users.password-change-url:http://localhost:4200/forgot-password}") String passwordChangeUrl) {
        this.authServiceClient = authServiceClient;
        this.userProfileService = userProfileService;
        this.walletRepository = walletRepository;
        this.temporaryPasswordService = temporaryPasswordService;
        this.credentialsNotificationService = credentialsNotificationService;
        this.cinemaServiceClient = cinemaServiceClient;
        this.userEventsPublisher = userEventsPublisher;
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
        String normalizedName = request.name().trim();
        userProfileService.createProfileAndWallet(created.id(), normalizedName, request.phone());
        publishAdminCreatedUserEvent(created, normalizedName, request.phone(), request.companyName());
        assignCinemaIfRequested(created.id(), request);
        credentialsNotificationService.sendWelcomeCredentials(request.name(), request.email(), temporaryPassword, passwordChangeUrl);
        return mapCreatedUser(created, request);
    }

    @Override
    public List<AdminUserResponse> listUsers() {
        return authServiceClient.listUsers().stream().map(this::mapUser).toList();
    }

    @Override
    public List<AdminUserResponse> listUnassignedCinemaAdmins() {
        return authServiceClient.listUsers().stream()
                .filter(this::isCinemaAdmin)
                .filter(user -> !cinemaServiceClient.hasCinemaAssigned(user.id()))
                .map(this::mapUser)
                .toList();
    }

    @Override
    public void assignCinemaAdmin(UUID userId, UUID cinemaId) {
        AuthUserResponse user = authServiceClient.findUserById(userId)
                .orElseThrow(() -> new UserProfileNotFoundException("Usuario no encontrado"));

        validateCinemaAdmin(user);
        cinemaServiceClient.assignCinemaAdmin(cinemaId, userId);
    }

    @Override
    public void deactivateUser(UUID userId) {
        authServiceClient.deactivateUser(userId);
    }

    @Override
    public void activateUser(UUID userId) {
        authServiceClient.activateUser(userId);
    }

    private void assignCinemaIfRequested(UUID userId, AdminCreateUserRequest request) {
        if (request.role() != UserRole.CINEMA_ADMIN || request.cinemaId() == null) {
            return;
        }
        cinemaServiceClient.assignCinemaAdmin(request.cinemaId(), userId);
    }

    private boolean isCinemaAdmin(AuthUserResponse user) {
        return UsersConstants.ROLE_CINEMA_ADMIN.equalsIgnoreCase(user.role()) && user.active();
    }

    private void validateCinemaAdmin(AuthUserResponse user) {
        if (!isCinemaAdmin(user)) {
            throw new InvalidAdminOperationException("Solo usuarios CINEMA_ADMIN activos se pueden asignar a un cine");
        }
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
                UserRole.SYSTEM_ADMIN,
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

    private void publishAdminCreatedUserEvent(AuthCreateUserResponse created, String name, String phone, String companyName) {
        UserCreatedEvent event = new UserCreatedEvent(
                resolveEventName(created.role()),
                created.id().toString(),
                name,
                phone,
                resolveCompanyNameForEvent(created.role(), companyName)
        );
        try {
            userEventsPublisher.publish(event);
        } catch (Exception exception) {
            log.error("Error publicando evento de creacion por admin para userId={} role={}", created.id(), created.role(), exception);
        }
    }

    private String resolveEventName(String role) {
        if (UsersConstants.ROLE_CINEMA_ADMIN.equals(role)) {
            return UsersConstants.EVENT_CINEMA_ADMIN_CREATED;
        }
        if (UserRole.ADVERTISER.name().equals(role)) {
            return UsersConstants.EVENT_ADVERTISER_CREATED;
        }
        return UsersConstants.EVENT_USER_CREATED;
    }

    private String resolveCompanyNameForEvent(String role, String companyName) {
        if (!UsersConstants.ROLE_CINEMA_ADMIN.equals(role)) {
            return null;
        }
        return normalizeOptional(companyName);
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
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
        BigDecimal balance = walletRepository.findByUserId(authUser.id())
                .map(wallet -> wallet.getBalance())
                .orElse(UsersConstants.DEFAULT_WALLET_BALANCE);
        String name = profile == null ? null : profile.getName();
        String phone = profile == null ? null : profile.getPhone();
        return new AdminUserResponse(authUser.id(), name, phone, authUser.email(), authUser.role(), authUser.active(), balance);
    }
}
