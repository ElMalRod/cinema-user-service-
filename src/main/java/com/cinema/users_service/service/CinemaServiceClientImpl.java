package com.cinema.users_service.service;

import com.cinema.users_service.constants.UsersConstants;
import com.cinema.users_service.exception.ExternalServiceException;
import com.cinema.users_service.exception.InvalidAdminOperationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Service
public class CinemaServiceClientImpl implements CinemaServiceClient {

    private final RestClient restClient;
    private final String baseUrl;

    public CinemaServiceClientImpl(RestClient.Builder builder,
                                   @Value("${users.cinema.base-url:http://98.80.232.250}") String baseUrl) {
        this.restClient = builder.build();
        this.baseUrl = baseUrl;
    }

    @Override
    public boolean hasCinemaAssigned(UUID adminUserId) {
        try {
            restClient.get()
                    .uri(baseUrl + "/cinemas/v1/cinemas/admin/" + adminUserId)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (HttpStatusCodeException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return false;
            }
            throw new ExternalServiceException("No fue posible consultar asignaciones en cinema-service");
        } catch (Exception exception) {
            throw new ExternalServiceException("No fue posible consultar asignaciones en cinema-service");
        }
    }

    @Override
    public void assignCinemaAdmin(UUID cinemaId, UUID adminUserId) {
        try {
            restClient.patch()
                    .uri(baseUrl + "/cinemas/v1/cinemas/" + cinemaId + "/admin")
                    .body(new CinemaAdminAssignmentRequest(adminUserId))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpStatusCodeException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new InvalidAdminOperationException("El cine indicado no existe");
            }
            if (exception.getStatusCode() == HttpStatus.CONFLICT) {
                throw new InvalidAdminOperationException("El administrador ya esta asignado a otro cine");
            }
            throw new ExternalServiceException("No fue posible asignar el administrador en cinema-service");
        } catch (Exception exception) {
            throw new ExternalServiceException("No fue posible asignar el administrador en cinema-service");
        }
    }

    private record CinemaAdminAssignmentRequest(UUID adminCinemaId) {
    }
}
