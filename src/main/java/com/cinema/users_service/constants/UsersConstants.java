package com.cinema.users_service.constants;

import java.math.BigDecimal;

public final class UsersConstants {

    public static final String EVENT_USER_CREATED = "USER_CREATED";
    public static final String KAFKA_TOPIC_USER_EVENTS = "user-events";
    public static final BigDecimal DEFAULT_WALLET_BALANCE = BigDecimal.ZERO;

    private UsersConstants() {
    }
}
