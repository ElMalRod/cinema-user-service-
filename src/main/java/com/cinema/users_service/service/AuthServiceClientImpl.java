package com.cinema.users_service.service;

import com.cinema.users_service.constants.UsersConstants;
import com.cinema.users_service.dto.auth.AuthCreateUserRequest;
import com.cinema.users_service.dto.auth.AuthCreateUserResponse;
import com.cinema.users_service.dto.auth.AuthUserResponse;
import com.cinema.users_service.exception.DuplicateUserException;
import com.cinema.users_service.exception.ExternalServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthServiceClientImpl implements AuthServiceClient {

    private final RestClient restClient;
    private final String baseUrl;

    public AuthServiceClientImpl(RestClient.Builder builder,
                                 @Value("${users.auth.base-url:http://auth-service:8081}") String baseUrl) {
        this.restClient = builder.build();
        this.baseUrl = baseUrl;
    }

    @Override
    public boolean existsSystemAdmin() {
        return restClient.get()
                .uri(baseUrl + "/auth/exists-admin")
                .header(UsersConstants.HEADER_INTERNAL_SERVICE, UsersConstants.HEADER_INTERNAL_SERVICE_VALUE)
                .retrieve()
                .body(Boolean.class);
    }

    @Override
    public AuthCreateUserResponse createUser(AuthCreateUserRequest request) {
        try {
            return restClient.post()
                    .uri(baseUrl + "/auth/admin/create-user")
                    .header(UsersConstants.HEADER_INTERNAL_SERVICE, UsersConstants.HEADER_INTERNAL_SERVICE_VALUE)
                    .body(request)
                    .retrieve()
                    .body(AuthCreateUserResponse.class);
        } catch (HttpStatusCodeException exception) {
            if (exception.getStatusCode() == HttpStatus.CONFLICT) {
                throw new DuplicateUserException("El email ya existe en auth-service");
            }
            throw new ExternalServiceException("No fue posible crear el usuario en auth-service");
        } catch (Exception exception) {
            throw new ExternalServiceException("No fue posible crear el usuario en auth-service");
        }
    }

    @Override
    public List<AuthUserResponse> listUsers() {
        try {
            List<AuthUserResponse> response = restClient.get()
                    .uri(baseUrl + "/auth/admin/list")
                    .header(UsersConstants.HEADER_INTERNAL_SERVICE, UsersConstants.HEADER_INTERNAL_SERVICE_VALUE)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<AuthUserResponse>>() {
                    });
            return response == null ? List.of() : response;
        } catch (Exception exception) {
            throw new ExternalServiceException("No fue posible listar usuarios en auth-service");
        }
    }

    @Override
    public Optional<AuthUserResponse> findUserById(UUID userId) {
        return listUsers().stream().filter(user -> user.id().equals(userId)).findFirst();
    }

    @Override
    public void deactivateUser(UUID userId) {
        callActivationEndpoint(baseUrl + "/auth/admin/deactivate/" + userId);
    }

    @Override
    public void activateUser(UUID userId) {
        callActivationEndpoint(baseUrl + "/auth/admin/activate/" + userId);
    }

    private void callActivationEndpoint(String url) {
        try {
            restClient.patch()
                    .uri(url)
                    .header(UsersConstants.HEADER_INTERNAL_SERVICE, UsersConstants.HEADER_INTERNAL_SERVICE_VALUE)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception exception) {
            throw new ExternalServiceException("No fue posible actualizar el estado del usuario en auth-service");
        }
    }
}