package com.cinema.users_service.service;

import java.util.UUID;

public interface CinemaServiceClient {

    boolean hasCinemaAssigned(UUID adminUserId);

    void assignCinemaAdmin(UUID cinemaId, UUID adminUserId);
}
