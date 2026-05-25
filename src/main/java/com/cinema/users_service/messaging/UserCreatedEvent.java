package com.cinema.users_service.messaging;

public record UserCreatedEvent(
        String event,
        String id,
        String name,
        String phone,
        String companyName
) {
}
