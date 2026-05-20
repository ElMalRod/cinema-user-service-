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

    @KafkaListener(topics = "${users.kafka.topic-user-events:user-events}")
    public void onMessage(UserCreatedEvent event) {
        if (!UsersConstants.EVENT_USER_CREATED.equals(event.event())) {
            return;
        }
        userBootstrapService.createFromEvent(event);
        log.info("Processed USER_CREATED for user {}", event.id());
    }
}
