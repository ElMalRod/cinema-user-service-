package com.cinema.users_service.controller;

import com.cinema.users_service.constants.UsersConstants;
import com.cinema.users_service.dto.profile.ProfileResponse;
import com.cinema.users_service.dto.profile.UpdateProfileRequest;
import com.cinema.users_service.service.HeaderAccessService;
import com.cinema.users_service.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/users/profile")
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final HeaderAccessService headerAccessService;

    public UserProfileController(UserProfileService userProfileService, HeaderAccessService headerAccessService) {
        this.userProfileService = userProfileService;
        this.headerAccessService = headerAccessService;
    }

    @GetMapping
    public ResponseEntity<ProfileResponse> getProfile(
            @RequestHeader(UsersConstants.HEADER_USER_ID) String userIdHeader,
            @RequestHeader(UsersConstants.HEADER_USER_ROLE) String userRole
    ) {
        UUID userId = headerAccessService.parseUserId(userIdHeader);
        return ResponseEntity.ok(userProfileService.getProfile(userId, userRole));
    }

    @PatchMapping
    public ResponseEntity<ProfileResponse> updateProfile(
            @RequestHeader(UsersConstants.HEADER_USER_ID) String userIdHeader,
            @RequestHeader(UsersConstants.HEADER_USER_ROLE) String userRole,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        UUID userId = headerAccessService.parseUserId(userIdHeader);
        return ResponseEntity.ok(userProfileService.updateProfile(userId, userRole, request));
    }
}