package com.cinema.users_service.service;

import com.cinema.users_service.constants.UsersConstants;
import com.cinema.users_service.messaging.UserCreatedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class UserEventsPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String userEventsTopic;

    public UserEventsPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${users.kafka.topic-user-events:" + UsersConstants.KAFKA_TOPIC_USER_EVENTS + "}") String userEventsTopic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.userEventsTopic = userEventsTopic;
    }

    public void publish(UserCreatedEvent event) {
        kafkaTemplate.send(userEventsTopic, event);
    }
}
