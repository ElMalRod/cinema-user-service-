package com.cinema.users_service.service;

import com.cinema.users_service.constants.UsersConstants;
import com.cinema.users_service.exception.ExternalServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class BrevoCredentialsNotificationService implements CredentialsNotificationService {

    private final RestClient restClient;
    private final boolean enabled;
    private final String apiKey;
    private final String senderEmail;
    private final String senderName;

    public BrevoCredentialsNotificationService(
            RestClient.Builder builder,
            @Value("${notifications.brevo.enabled:true}") boolean enabled,
            @Value("${notifications.brevo.api-key:}") String apiKey,
            @Value("${notifications.brevo.sender-email:noreply@cinema.local}") String senderEmail,
            @Value("${notifications.brevo.sender-name:cinema}") String senderName
    ) {
        this.restClient = builder.baseUrl(UsersConstants.BREVO_BASE_URL).build();
        this.enabled = enabled;
        this.apiKey = apiKey;
        this.senderEmail = senderEmail;
        this.senderName = senderName;
    }

    @Override
    public void sendWelcomeCredentials(String name, String email, String temporaryPassword, String resetLink) {
        if (!enabled || apiKey == null || apiKey.isBlank()) {
            log.info("Brevo disabled or api key missing. Welcome credentials for {} were not sent", email);
            return;
        }
        Map<String, Object> payload = buildPayload(name, email, temporaryPassword, resetLink);
        try {
            restClient.post()
                    .uri(UsersConstants.BREVO_EMAIL_PATH)
                    .header(UsersConstants.BREVO_API_KEY_HEADER, apiKey)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception exception) {
            throw new ExternalServiceException("No fue posible enviar el correo de credenciales");
        }
    }

    private Map<String, Object> buildPayload(String name, String email, String temporaryPassword, String resetLink) {
        String content = "<h3>Bienvenido a Cinema App</h3>"
                + "<p>Hola " + name + ",</p>"
                + "<p>Tus credenciales temporales son:</p>"
                + "<ul><li>Email: " + email + "</li><li>Password temporal: " + temporaryPassword + "</li></ul>"
                + "<p>Debes cambiar tu password en el primer login.</p>"
                + "<p>Link: <a href='" + resetLink + "'>" + resetLink + "</a></p>";
        return Map.of(
                "sender", Map.of("email", senderEmail, "name", senderName),
                "to", List.of(Map.of("email", email, "name", name)),
                "subject", UsersConstants.WELCOME_EMAIL_SUBJECT,
                "htmlContent", content
        );
    }
}