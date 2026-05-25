package com.cinema.users_service.controller;

import com.cinema.users_service.constants.UsersConstants;
import com.cinema.users_service.dto.admin.AdminCreateUserRequest;
import com.cinema.users_service.dto.admin.AdminUserResponse;
import com.cinema.users_service.dto.admin.AssignCinemaAdminRequest;
import com.cinema.users_service.service.AdminUserService;
import com.cinema.users_service.service.HeaderAccessService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users/admin")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final HeaderAccessService headerAccessService;

    public AdminUserController(AdminUserService adminUserService, HeaderAccessService headerAccessService) {
        this.adminUserService = adminUserService;
        this.headerAccessService = headerAccessService;
    }

    @PostMapping("/create")
    public ResponseEntity<AdminUserResponse> createUser(
            @RequestHeader(UsersConstants.HEADER_USER_ROLE) String userRole,
            @Valid @RequestBody AdminCreateUserRequest request
    ) {
        headerAccessService.requireSystemAdmin(userRole);
        return ResponseEntity.status(HttpStatus.CREATED).body(adminUserService.createUser(request));
    }

    @GetMapping("/list")
    public ResponseEntity<List<AdminUserResponse>> listUsers(
            @RequestHeader(UsersConstants.HEADER_USER_ROLE) String userRole
    ) {
        headerAccessService.requireSystemAdmin(userRole);
        return ResponseEntity.ok(adminUserService.listUsers());
    }

    @GetMapping("/cinema-admins/unassigned")
    public ResponseEntity<List<AdminUserResponse>> listUnassignedCinemaAdmins(
            @RequestHeader(UsersConstants.HEADER_USER_ROLE) String userRole
    ) {
        headerAccessService.requireSystemAdmin(userRole);
        return ResponseEntity.ok(adminUserService.listUnassignedCinemaAdmins());
    }

    @PatchMapping("/cinema-admins/{userId}/assign")
    public ResponseEntity<Void> assignCinemaAdmin(
            @RequestHeader(UsersConstants.HEADER_USER_ROLE) String userRole,
            @PathVariable UUID userId,
            @Valid @RequestBody AssignCinemaAdminRequest request
    ) {
        headerAccessService.requireSystemAdmin(userRole);
        adminUserService.assignCinemaAdmin(userId, request.cinemaId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{userId}/deactivate")
    public ResponseEntity<Void> deactivateUser(
            @RequestHeader(UsersConstants.HEADER_USER_ROLE) String userRole,
            @PathVariable UUID userId
    ) {
        headerAccessService.requireSystemAdmin(userRole);
        adminUserService.deactivateUser(userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{userId}/activate")
    public ResponseEntity<Void> activateUser(
            @RequestHeader(UsersConstants.HEADER_USER_ROLE) String userRole,
            @PathVariable UUID userId
    ) {
        headerAccessService.requireSystemAdmin(userRole);
        adminUserService.activateUser(userId);
        return ResponseEntity.noContent().build();
    }
}
