package com.cinema.users_service.constants;

import java.math.BigDecimal;
import java.util.Set;

public final class UsersConstants {

    public static final String EVENT_USER_CREATED = "USER_CREATED";
    public static final String KAFKA_TOPIC_USER_EVENTS = "user-events";

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_ROLE = "X-User-Role";
    public static final String HEADER_INTERNAL_SERVICE = "X-Internal-Service";
    public static final String HEADER_INTERNAL_SERVICE_VALUE = "true";

    public static final String ROLE_SYSTEM_ADMIN = "SYSTEM_ADMIN";
    public static final Set<String> ADMIN_MANAGED_ROLES = Set.of(
            "SYSTEM_ADMIN",
            "CINEMA_ADMIN",
            "CLIENT",
            "ADVERTISER"
    );

    public static final String DEFAULT_SYSTEM_ADMIN_EMAIL = "admin@cinema.com";
    public static final String DEFAULT_SYSTEM_ADMIN_PASSWORD = "123456789";
    public static final String DEFAULT_SYSTEM_ADMIN_NAME = "System Admin";

    public static final String BREVO_BASE_URL = "https://api.brevo.com/v3";
    public static final String BREVO_EMAIL_PATH = "/smtp/email";
    public static final String BREVO_API_KEY_HEADER = "api-key";
    public static final String WELCOME_EMAIL_SUBJECT = "Bienvenido a Cinema App - Credenciales de acceso";

    public static final String RECHARGE_DESCRIPTION = "Recarga de wallet";
    public static final BigDecimal DEFAULT_WALLET_BALANCE = BigDecimal.ZERO;
    public static final int LAST_TRANSACTIONS_LIMIT = 10;

    private UsersConstants() {
    }
}