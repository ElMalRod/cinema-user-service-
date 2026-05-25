package com.cinema.users_service.service;

import com.cinema.users_service.constants.UsersConstants;
import com.cinema.users_service.domain.UserRole;
import com.cinema.users_service.dto.auth.AuthCreateUserRequest;
import com.cinema.users_service.dto.auth.AuthCreateUserResponse;
import com.cinema.users_service.dto.auth.AuthUserResponse;
import com.cinema.users_service.exception.DuplicateUserException;
import com.cinema.users_service.exception.ExternalServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class AuthServiceClientImplTest {

    private static final String BASE_URL = "http://auth-service";
    private static final String EXISTS_ADMIN_URI = BASE_URL + "/auth/exists-admin";
    private static final String CREATE_USER_URI = BASE_URL + "/auth/admin/create-user";
    private static final String LIST_USERS_URI = BASE_URL + "/auth/admin/list";
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000801");
    private static final UUID OTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000802");
    private static final String USER_EMAIL = "client@test.com";
    private static final String OTHER_USER_EMAIL = "admin@test.com";
    private static final String ROLE_CLIENT = "CLIENT";
    private static final String ROLE_SYSTEM_ADMIN = "SYSTEM_ADMIN";
    private static final String PASSWORD = "TempPass123!";
    private static final String CREATE_USER_ERROR_MESSAGE = "No fue posible crear el usuario en auth-service";
    private static final String LIST_USERS_ERROR_MESSAGE = "No fue posible listar usuarios en auth-service";
    private static final String ACTIVATE_DEACTIVATE_ERROR_MESSAGE = "No fue posible actualizar el estado del usuario en auth-service";
    private static final String LIST_USERS_JSON = """
            [
              {"id":"00000000-0000-0000-0000-000000000801","email":"client@test.com","role":"CLIENT","active":true},
              {"id":"00000000-0000-0000-0000-000000000802","email":"admin@test.com","role":"SYSTEM_ADMIN","active":false}
            ]
            """;
    private static final String CREATE_USER_JSON = """
            {"id":"00000000-0000-0000-0000-000000000801","email":"client@test.com","role":"CLIENT","active":true,"requiresPasswordChange":true}
            """;

    private MockRestServiceServer mockRestServiceServer;
    private AuthServiceClientImpl authServiceClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockRestServiceServer = MockRestServiceServer.bindTo(builder).build();
        authServiceClient = new AuthServiceClientImpl(builder, BASE_URL);
    }

    @Test
    void should_ReturnTrue_When_SystemAdminExists() {
        // Arrange
        mockRestServiceServer.expect(requestTo(EXISTS_ADMIN_URI))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(UsersConstants.HEADER_INTERNAL_SERVICE, UsersConstants.HEADER_INTERNAL_SERVICE_VALUE))
                .andRespond(withSuccess("true", MediaType.APPLICATION_JSON));

        // Act
        boolean exists = authServiceClient.existsSystemAdmin();

        // Assert
        assertTrue(exists);
        mockRestServiceServer.verify();
    }

    @Test
    void should_CreateUser_When_RequestIsValid() {
        // Arrange
        AuthCreateUserRequest request = buildCreateUserRequest();
        mockRestServiceServer.expect(requestTo(CREATE_USER_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(UsersConstants.HEADER_INTERNAL_SERVICE, UsersConstants.HEADER_INTERNAL_SERVICE_VALUE))
                .andRespond(withSuccess(CREATE_USER_JSON, MediaType.APPLICATION_JSON));

        // Act
        AuthCreateUserResponse response = authServiceClient.createUser(request);

        // Assert
        assertEquals(USER_ID, response.id());
        assertEquals(USER_EMAIL, response.email());
        assertEquals(ROLE_CLIENT, response.role());
        assertTrue(response.active());
        assertTrue(response.requiresPasswordChange());
        mockRestServiceServer.verify();
    }

    @Test
    void should_ThrowDuplicateUserException_When_CreateUserReturnsConflict() {
        // Arrange
        AuthCreateUserRequest request = buildCreateUserRequest();
        mockRestServiceServer.expect(requestTo(CREATE_USER_URI))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CONFLICT));

        // Act
        DuplicateUserException exception = assertThrows(
                DuplicateUserException.class,
                () -> authServiceClient.createUser(request)
        );

        // Assert
        assertEquals("El email ya existe en auth-service", exception.getMessage());
    }

    @Test
    void should_ThrowExternalServiceException_When_CreateUserReturnsUnexpectedError() {
        // Arrange
        AuthCreateUserRequest request = buildCreateUserRequest();
        mockRestServiceServer.expect(requestTo(CREATE_USER_URI))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        // Act
        ExternalServiceException exception = assertThrows(
                ExternalServiceException.class,
                () -> authServiceClient.createUser(request)
        );

        // Assert
        assertEquals(CREATE_USER_ERROR_MESSAGE, exception.getMessage());
    }

    @Test
    void should_ReturnUsers_When_ListUsersSucceeds() {
        // Arrange
        mockRestServiceServer.expect(requestTo(LIST_USERS_URI))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(UsersConstants.HEADER_INTERNAL_SERVICE, UsersConstants.HEADER_INTERNAL_SERVICE_VALUE))
                .andRespond(withSuccess(LIST_USERS_JSON, MediaType.APPLICATION_JSON));

        // Act
        List<AuthUserResponse> users = authServiceClient.listUsers();

        // Assert
        assertEquals(2, users.size());
        assertEquals(USER_ID, users.getFirst().id());
        assertEquals(OTHER_USER_ID, users.get(1).id());
        mockRestServiceServer.verify();
    }

    @Test
    void should_ReturnEmptyList_When_ListUsersBodyIsNull() {
        // Arrange
        mockRestServiceServer.expect(requestTo(LIST_USERS_URI))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withNoContent());

        // Act
        List<AuthUserResponse> users = authServiceClient.listUsers();

        // Assert
        assertTrue(users.isEmpty());
    }

    @Test
    void should_ThrowExternalServiceException_When_ListUsersFails() {
        // Arrange
        mockRestServiceServer.expect(requestTo(LIST_USERS_URI))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        // Act
        ExternalServiceException exception = assertThrows(
                ExternalServiceException.class,
                () -> authServiceClient.listUsers()
        );

        // Assert
        assertEquals(LIST_USERS_ERROR_MESSAGE, exception.getMessage());
    }

    @Test
    void should_ReturnOptionalWithUser_When_FindUserByIdExists() {
        // Arrange
        mockRestServiceServer.expect(requestTo(LIST_USERS_URI))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(LIST_USERS_JSON, MediaType.APPLICATION_JSON));

        // Act
        Optional<AuthUserResponse> user = authServiceClient.findUserById(USER_ID);

        // Assert
        assertTrue(user.isPresent());
        assertEquals(USER_ID, user.get().id());
    }

    @Test
    void should_ReturnEmptyOptional_When_FindUserByIdDoesNotExist() {
        // Arrange
        UUID unknownUserId = UUID.fromString("00000000-0000-0000-0000-000000000803");
        mockRestServiceServer.expect(requestTo(LIST_USERS_URI))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(LIST_USERS_JSON, MediaType.APPLICATION_JSON));

        // Act
        Optional<AuthUserResponse> user = authServiceClient.findUserById(unknownUserId);

        // Assert
        assertFalse(user.isPresent());
    }

    @Test
    void should_DeactivateUser_When_RequestSucceeds() {
        // Arrange
        String deactivateUri = BASE_URL + "/auth/admin/deactivate/" + USER_ID;
        mockRestServiceServer.expect(requestTo(deactivateUri))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(header(UsersConstants.HEADER_INTERNAL_SERVICE, UsersConstants.HEADER_INTERNAL_SERVICE_VALUE))
                .andRespond(withStatus(HttpStatus.OK));

        // Act
        authServiceClient.deactivateUser(USER_ID);

        // Assert
        mockRestServiceServer.verify();
    }

    @Test
    void should_ThrowExternalServiceException_When_DeactivateUserFails() {
        // Arrange
        String deactivateUri = BASE_URL + "/auth/admin/deactivate/" + USER_ID;
        mockRestServiceServer.expect(requestTo(deactivateUri))
                .andExpect(method(HttpMethod.PATCH))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        // Act
        ExternalServiceException exception = assertThrows(
                ExternalServiceException.class,
                () -> authServiceClient.deactivateUser(USER_ID)
        );

        // Assert
        assertEquals(ACTIVATE_DEACTIVATE_ERROR_MESSAGE, exception.getMessage());
    }

    @Test
    void should_ActivateUser_When_RequestSucceeds() {
        // Arrange
        String activateUri = BASE_URL + "/auth/admin/activate/" + USER_ID;
        mockRestServiceServer.expect(requestTo(activateUri))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(header(UsersConstants.HEADER_INTERNAL_SERVICE, UsersConstants.HEADER_INTERNAL_SERVICE_VALUE))
                .andRespond(withStatus(HttpStatus.OK));

        // Act
        authServiceClient.activateUser(USER_ID);

        // Assert
        mockRestServiceServer.verify();
    }

    @Test
    void should_ThrowExternalServiceException_When_ActivateUserFails() {
        // Arrange
        String activateUri = BASE_URL + "/auth/admin/activate/" + USER_ID;
        mockRestServiceServer.expect(requestTo(activateUri))
                .andExpect(method(HttpMethod.PATCH))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        // Act
        ExternalServiceException exception = assertThrows(
                ExternalServiceException.class,
                () -> authServiceClient.activateUser(USER_ID)
        );

        // Assert
        assertEquals(ACTIVATE_DEACTIVATE_ERROR_MESSAGE, exception.getMessage());
    }

    private AuthCreateUserRequest buildCreateUserRequest() {
        return new AuthCreateUserRequest(
                "Client Name",
                "55551234",
                "Client Company",
                USER_EMAIL,
                PASSWORD,
                UserRole.CLIENT,
                true
        );
    }
}
