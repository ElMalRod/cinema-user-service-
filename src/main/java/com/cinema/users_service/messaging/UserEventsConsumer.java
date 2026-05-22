package com.cinema.users_service.messaging;

import com.cinema.users_service.constants.UsersConstants;
import com.cinema.users_service.service.UserBootstrapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventsConsumer {

    private final UserBootstrapService userBootstrapService;

    @KafkaListener(topics = "user-events", groupId = "users-service-group")
    public void consume(UserCreatedEvent payload) {
        log.info("Evento recibido: {} para usuario: {}", payload.event(), payload.id());
        if (!isProfileBootstrapEvent(payload.event())) {
            return;
        }
        userBootstrapService.createFromEvent(payload);
    }

    public void onMessage(UserCreatedEvent payload) {
        consume(payload);
    }

    private boolean isProfileBootstrapEvent(String event) {
        return UsersConstants.EVENT_USER_CREATED.equals(event)
                || UsersConstants.EVENT_CINEMA_ADMIN_CREATED.equals(event)
                || UsersConstants.EVENT_ADVERTISER_CREATED.equals(event);
    }
}
