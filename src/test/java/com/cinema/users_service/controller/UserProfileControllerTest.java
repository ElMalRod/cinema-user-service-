package com.cinema.users_service.controller;

import com.cinema.users_service.dto.profile.ProfileResponse;
import com.cinema.users_service.dto.profile.UpdateProfileRequest;
import com.cinema.users_service.service.HeaderAccessService;
import com.cinema.users_service.service.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileControllerTest {

    private static final String USER_ID_HEADER = "00000000-0000-0000-0000-000000000904";
    private static final UUID USER_ID = UUID.fromString(USER_ID_HEADER);
    private static final String USER_ROLE = "CLIENT";
    private static final String USER_NAME = "Profile Name";
    private static final String USER_PHONE = "5551234";
    private static final String USER_EMAIL = "profile@test.com";

    @Mock
    private UserProfileService userProfileService;

    @Mock
    private HeaderAccessService headerAccessService;

    private UserProfileController userProfileController;

    @BeforeEach
    void setUp() {
        userProfileController = new UserProfileController(userProfileService, headerAccessService);
    }

    @Test
    void should_ReturnProfile_When_GetProfileIsCalled() {
        // Arrange
        ProfileResponse profileResponse = new ProfileResponse(USER_ID, USER_NAME, USER_PHONE, USER_EMAIL, USER_ROLE);
        when(headerAccessService.parseUserId(USER_ID_HEADER)).thenReturn(USER_ID);
        when(userProfileService.getProfile(USER_ID, USER_ROLE)).thenReturn(profileResponse);

        // Act
        var result = userProfileController.getProfile(USER_ID_HEADER, USER_ROLE);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(USER_ID, result.getBody().userId());
        verify(headerAccessService).parseUserId(USER_ID_HEADER);
    }

    @Test
    void should_UpdateProfile_When_UpdateProfileIsCalled() {
        // Arrange
        UpdateProfileRequest request = new UpdateProfileRequest(USER_NAME, USER_PHONE);
        ProfileResponse profileResponse = new ProfileResponse(USER_ID, USER_NAME, USER_PHONE, USER_EMAIL, USER_ROLE);
        when(headerAccessService.parseUserId(USER_ID_HEADER)).thenReturn(USER_ID);
        when(userProfileService.updateProfile(USER_ID, USER_ROLE, request)).thenReturn(profileResponse);

        // Act
        var result = userProfileController.updateProfile(USER_ID_HEADER, USER_ROLE, request);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(USER_NAME, result.getBody().name());
        verify(userProfileService).updateProfile(USER_ID, USER_ROLE, request);
    }
}
