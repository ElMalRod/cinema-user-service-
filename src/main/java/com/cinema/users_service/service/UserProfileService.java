package com.cinema.users_service.service;

import com.cinema.users_service.domain.UserProfile;
import com.cinema.users_service.dto.profile.ProfileResponse;
import com.cinema.users_service.dto.profile.UpdateProfileRequest;

import java.util.Optional;
import java.util.UUID;

public interface UserProfileService {

    void createProfileAndWallet(UUID userId, String name, String phone);

    ProfileResponse getProfile(UUID userId, String role);

    ProfileResponse updateProfile(UUID userId, String role, UpdateProfileRequest request);

    Optional<UserProfile> findProfile(UUID userId);
}