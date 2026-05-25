package com.cinema.users_service.controller;

import com.cinema.users_service.dto.internal.UserSummaryResponse;
import com.cinema.users_service.exception.UserProfileNotFoundException;
import com.cinema.users_service.repository.UserProfileRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/users/internal")
public class InternalUserController {

    private final UserProfileRepository userProfileRepository;

    public InternalUserController(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserSummaryResponse> getUserSummary(@PathVariable UUID userId) {
        return userProfileRepository.findById(userId)
                .map(profile -> ResponseEntity.ok(new UserSummaryResponse(profile.getId(), profile.getName())))
                .orElseThrow(() -> new UserProfileNotFoundException("Usuario no encontrado: " + userId));
    }
}
