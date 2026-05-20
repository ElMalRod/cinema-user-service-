package com.cinema.users_service.service;

import com.cinema.users_service.messaging.UserCreatedEvent;

public interface UserBootstrapService {

    void createFromEvent(UserCreatedEvent event);
}
