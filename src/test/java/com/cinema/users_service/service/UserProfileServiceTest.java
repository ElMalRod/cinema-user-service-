package com.cinema.users_service.service;

import com.cinema.users_service.constants.UsersConstants;
import com.cinema.users_service.domain.UserProfile;
import com.cinema.users_service.domain.Wallet;
import com.cinema.users_service.dto.auth.AuthUserResponse;
import com.cinema.users_service.dto.profile.ProfileResponse;
import com.cinema.users_service.dto.profile.UpdateProfileRequest;
import com.cinema.users_service.exception.UserProfileNotFoundException;
import com.cinema.users_service.repository.UserProfileRepository;
import com.cinema.users_service.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private AuthServiceClient authServiceClient;

    @InjectMocks
    private UserProfileServiceImpl userProfileService;

    @Test
    void getExistingProfileShouldReturnCorrectData() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UserProfile profile = UserProfile.builder().id(userId).name("Ana").phone("5555").build();
        when(userProfileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(authServiceClient.findUserById(userId)).thenReturn(Optional.of(new AuthUserResponse(userId, "ana@test.com", "CLIENT", true)));

        // Act
        ProfileResponse response = userProfileService.getProfile(userId, "CLIENT");

        // Assert
        assertEquals("Ana", response.name());
        assertEquals("ana@test.com", response.email());
        assertEquals("CLIENT", response.role());
    }

    @Test
    void getProfileWithoutAuthDataShouldFallbackRoleAndNullEmail() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UserProfile profile = UserProfile.builder().id(userId).name("Carlos").phone("1234").build();
        when(userProfileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(authServiceClient.findUserById(userId)).thenReturn(Optional.empty());

        // Act
        ProfileResponse response = userProfileService.getProfile(userId, "CINEMA_ADMIN");

        // Assert
        assertNull(response.email());
        assertEquals("CINEMA_ADMIN", response.role());
    }

    @Test
    void getUnknownProfileShouldThrowException() {
        // Arrange
        UUID userId = UUID.randomUUID();
        when(userProfileRepository.findById(userId)).thenReturn(Optional.empty());

        // Act
        UserProfileNotFoundException exception = assertThrows(UserProfileNotFoundException.class,
                () -> userProfileService.getProfile(userId, "CLIENT"));

        // Assert
        assertEquals("Perfil de usuario no encontrado", exception.getMessage());
    }

    @Test
    void updateProfileShouldPersistChanges() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UserProfile profile = UserProfile.builder().id(userId).name("Ana").phone("1111").build();
        UpdateProfileRequest request = new UpdateProfileRequest("Ana Maria", "2222");
        when(userProfileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(authServiceClient.findUserById(userId)).thenReturn(Optional.of(new AuthUserResponse(userId, "ana@test.com", "CLIENT", true)));

        // Act
        ProfileResponse response = userProfileService.updateProfile(userId, "CLIENT", request);

        // Assert
        assertEquals("Ana Maria", response.name());
        assertEquals("2222", response.phone());
        verify(userProfileRepository).save(profile);
    }

    @Test
    void updateUnknownProfileShouldThrowException() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UpdateProfileRequest request = new UpdateProfileRequest("Ana Maria", "2222");
        when(userProfileRepository.findById(userId)).thenReturn(Optional.empty());

        // Act
        UserProfileNotFoundException exception = assertThrows(UserProfileNotFoundException.class,
                () -> userProfileService.updateProfile(userId, "CLIENT", request));

        // Assert
        assertEquals("Perfil de usuario no encontrado", exception.getMessage());
    }

    @Test
    void createProfileAndWalletWhenProfileDoesNotExistShouldCreateBoth() {
        // Arrange
        UUID userId = UUID.randomUUID();
        when(userProfileRepository.existsById(userId)).thenReturn(false);
        when(walletRepository.existsByUserId(userId)).thenReturn(false);

        // Act
        userProfileService.createProfileAndWallet(userId, "Nuevo", "7777");

        // Assert
        verify(userProfileRepository).save(any(UserProfile.class));
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    void createProfileAndWalletWhenProfileAndWalletExistShouldSkipCreation() {
        // Arrange
        UUID userId = UUID.randomUUID();
        when(userProfileRepository.existsById(userId)).thenReturn(true);
        when(walletRepository.existsByUserId(userId)).thenReturn(true);

        // Act
        userProfileService.createProfileAndWallet(userId, "Nuevo", "7777");

        // Assert
        verify(userProfileRepository, never()).save(any(UserProfile.class));
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    void createProfileAndWalletWhenProfileExistsButWalletMissingShouldCreateWallet() {
        // Arrange
        UUID userId = UUID.randomUUID();
        when(userProfileRepository.existsById(userId)).thenReturn(true);
        when(walletRepository.existsByUserId(userId)).thenReturn(false);

        // Act
        userProfileService.createProfileAndWallet(userId, "Nuevo", "7777");

        // Assert
        verify(userProfileRepository, never()).save(any(UserProfile.class));
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    void findProfileShouldDelegateToRepository() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UserProfile profile = UserProfile.builder().id(userId).name("Ana").phone("1111").build();
        when(userProfileRepository.findById(userId)).thenReturn(Optional.of(profile));

        // Act
        Optional<UserProfile> result = userProfileService.findProfile(userId);

        // Assert
        assertEquals("Ana", result.orElseThrow().getName());
    }
}

