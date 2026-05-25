package com.cinema.users_service.service;

import com.cinema.users_service.constants.UsersConstants;
import com.cinema.users_service.domain.UserProfile;
import com.cinema.users_service.domain.Wallet;
import com.cinema.users_service.dto.profile.ProfileResponse;
import com.cinema.users_service.dto.profile.UpdateProfileRequest;
import com.cinema.users_service.exception.UserProfileNotFoundException;
import com.cinema.users_service.repository.UserProfileRepository;
import com.cinema.users_service.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final WalletRepository walletRepository;
    private final AuthServiceClient authServiceClient;

    public UserProfileServiceImpl(UserProfileRepository userProfileRepository,
                                  WalletRepository walletRepository,
                                  AuthServiceClient authServiceClient) {
        this.userProfileRepository = userProfileRepository;
        this.walletRepository = walletRepository;
        this.authServiceClient = authServiceClient;
    }

    @Override
    @Transactional
    public void createProfileAndWallet(UUID userId, String name, String phone) {
        if (userProfileRepository.existsById(userId)) {
            ensureWallet(userId);
            return;
        }
        UserProfile profile = UserProfile.builder().id(userId).name(name).phone(phone).build();
        userProfileRepository.save(profile);
        ensureWallet(userId);
    }

    @Override
    public ProfileResponse getProfile(UUID userId, String role) {
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new UserProfileNotFoundException("Perfil de usuario no encontrado"));
        String email = authServiceClient.findUserById(userId).map(user -> user.email()).orElse(null);
        String resolvedRole = authServiceClient.findUserById(userId).map(user -> user.role()).orElse(role);
        return new ProfileResponse(profile.getId(), profile.getName(), profile.getPhone(), email, resolvedRole);
    }

    @Override
    @Transactional
    public ProfileResponse updateProfile(UUID userId, String role, UpdateProfileRequest request) {
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new UserProfileNotFoundException("Perfil de usuario no encontrado"));
        profile.setName(request.name().trim());
        profile.setPhone(request.phone());
        userProfileRepository.save(profile);
        String email = authServiceClient.findUserById(userId).map(user -> user.email()).orElse(null);
        return new ProfileResponse(profile.getId(), profile.getName(), profile.getPhone(), email, role);
    }

    @Override
    public Optional<UserProfile> findProfile(UUID userId) {
        return userProfileRepository.findById(userId);
    }

    private void ensureWallet(UUID userId) {
        if (walletRepository.existsByUserId(userId)) {
            return;
        }
        Wallet wallet = Wallet.builder().userId(userId).balance(UsersConstants.DEFAULT_WALLET_BALANCE).build();
        walletRepository.save(wallet);
    }
}
