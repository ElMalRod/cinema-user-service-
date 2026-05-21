package com.cinema.users_service.service;

import com.cinema.users_service.dto.admin.AdminCreateUserRequest;
import com.cinema.users_service.dto.admin.AdminUserResponse;

import java.util.List;
import java.util.UUID;

public interface AdminUserService {

    void ensureInitialSystemAdmin();

    AdminUserResponse createUser(AdminCreateUserRequest request);

    List<AdminUserResponse> listUsers();

    void deactivateUser(UUID userId);

    void activateUser(UUID userId);
}