package com.cinema.users_service.service;

import com.cinema.users_service.exception.ExternalServiceException;
import com.cinema.users_service.exception.InvalidAdminOperationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@ExtendWith(MockitoExtension.class)
class CinemaServiceClientImplTest {

    private static final String BASE_URL = "http://cinema-service";
    private static final UUID ADMIN_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000905");
    private static final UUID CINEMA_ID = UUID.fromString("00000000-0000-0000-0000-000000000906");
    private static final String HAS_CINEMA_URI = BASE_URL + "/cinemas/v1/cinemas/admin/" + ADMIN_USER_ID;
    private static final String ASSIGN_URI = BASE_URL + "/cinemas/v1/cinemas/" + CINEMA_ID + "/admin";

    private MockRestServiceServer mockRestServiceServer;
    private CinemaServiceClientImpl cinemaServiceClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockRestServiceServer = MockRestServiceServer.bindTo(builder).build();
        cinemaServiceClient = new CinemaServiceClientImpl(builder, BASE_URL);
    }

    @Test
    void should_ReturnTrue_When_CinemaIsAssigned() {
        // Arrange
        mockRestServiceServer.expect(requestTo(HAS_CINEMA_URI))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK));

        // Act
        boolean assigned = cinemaServiceClient.hasCinemaAssigned(ADMIN_USER_ID);

        // Assert
        assertTrue(assigned);
        mockRestServiceServer.verify();
    }

    @Test
    void should_ReturnFalse_When_CinemaAssignmentIsNotFound() {
        // Arrange
        mockRestServiceServer.expect(requestTo(HAS_CINEMA_URI))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        // Act
        boolean assigned = cinemaServiceClient.hasCinemaAssigned(ADMIN_USER_ID);

        // Assert
        assertFalse(assigned);
        mockRestServiceServer.verify();
    }

    @Test
    void should_ThrowExternalServiceException_When_HasCinemaAssignedFailsWithUnexpectedError() {
        // Arrange
        mockRestServiceServer.expect(requestTo(HAS_CINEMA_URI))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        // Act
        ExternalServiceException exception = assertThrows(
                ExternalServiceException.class,
                () -> cinemaServiceClient.hasCinemaAssigned(ADMIN_USER_ID)
        );

        // Assert
        assertEquals("No fue posible consultar asignaciones en cinema-service", exception.getMessage());
    }

    @Test
    void should_AssignCinemaAdmin_When_RequestIsValid() {
        // Arrange
        mockRestServiceServer.expect(requestTo(ASSIGN_URI))
                .andExpect(method(HttpMethod.PATCH))
                .andRespond(withStatus(HttpStatus.OK));

        // Act
        cinemaServiceClient.assignCinemaAdmin(CINEMA_ID, ADMIN_USER_ID);

        // Assert
        mockRestServiceServer.verify();
    }

    @Test
    void should_ThrowInvalidAdminOperationException_When_CinemaDoesNotExist() {
        // Arrange
        mockRestServiceServer.expect(requestTo(ASSIGN_URI))
                .andExpect(method(HttpMethod.PATCH))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        // Act
        InvalidAdminOperationException exception = assertThrows(
                InvalidAdminOperationException.class,
                () -> cinemaServiceClient.assignCinemaAdmin(CINEMA_ID, ADMIN_USER_ID)
        );

        // Assert
        assertEquals("El cine indicado no existe", exception.getMessage());
    }

    @Test
    void should_ThrowInvalidAdminOperationException_When_AdminIsAlreadyAssigned() {
        // Arrange
        mockRestServiceServer.expect(requestTo(ASSIGN_URI))
                .andExpect(method(HttpMethod.PATCH))
                .andRespond(withStatus(HttpStatus.CONFLICT));

        // Act
        InvalidAdminOperationException exception = assertThrows(
                InvalidAdminOperationException.class,
                () -> cinemaServiceClient.assignCinemaAdmin(CINEMA_ID, ADMIN_USER_ID)
        );

        // Assert
        assertEquals("El administrador ya esta asignado a otro cine", exception.getMessage());
    }

    @Test
    void should_ThrowExternalServiceException_When_AssignCinemaAdminFailsWithUnexpectedError() {
        // Arrange
        mockRestServiceServer.expect(requestTo(ASSIGN_URI))
                .andExpect(method(HttpMethod.PATCH))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        // Act
        ExternalServiceException exception = assertThrows(
                ExternalServiceException.class,
                () -> cinemaServiceClient.assignCinemaAdmin(CINEMA_ID, ADMIN_USER_ID)
        );

        // Assert
        assertEquals("No fue posible asignar el administrador en cinema-service", exception.getMessage());
    }
}
