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
        log.info("Evento recibido: {} para usuario: {}",
                 payload.event(), payload.id());
        if (!UsersConstants.EVENT_USER_CREATED.equals(payload.event())) {
            return;
        }
        userBootstrapService.createFromEvent(payload);
        // lógica existente
    }
    public void onMessage(UserCreatedEvent payload) {
        consume(payload);
    }
}
