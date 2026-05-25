package com.cinema.users_service.service;

import com.cinema.users_service.dto.auth.AuthCreateUserRequest;
import com.cinema.users_service.dto.auth.AuthCreateUserResponse;
import com.cinema.users_service.dto.auth.AuthUserResponse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuthServiceClient {

    boolean existsSystemAdmin();

    AuthCreateUserResponse createUser(AuthCreateUserRequest request);

    List<AuthUserResponse> listUsers();

    Optional<AuthUserResponse> findUserById(UUID userId);

    void deactivateUser(UUID userId);

    void activateUser(UUID userId);
}