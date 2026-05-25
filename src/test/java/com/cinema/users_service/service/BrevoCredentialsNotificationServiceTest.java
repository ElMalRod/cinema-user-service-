package com.cinema.users_service.service;

import com.cinema.users_service.constants.UsersConstants;
import com.cinema.users_service.exception.ExternalServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@ExtendWith(MockitoExtension.class)
class BrevoCredentialsNotificationServiceTest {

    private static final boolean ENABLED = true;
    private static final boolean DISABLED = false;
    private static final String VALID_API_KEY = "api-key-value";
    private static final String BLANK_API_KEY = "   ";
    private static final String SENDER_EMAIL = "noreply@cinema.local";
    private static final String SENDER_NAME = "cinema";
    private static final String USER_NAME = "User Name";
    private static final String USER_EMAIL = "user@test.com";
    private static final String TEMPORARY_PASSWORD = "TempPass123";
    private static final String RESET_LINK = "https://frontend/reset-password";

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockRestServiceServer;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();
        mockRestServiceServer = MockRestServiceServer.bindTo(restClientBuilder).build();
    }

    @Test
    void should_NotSendEmail_When_BrevoIsDisabled() {
        // Arrange
        BrevoCredentialsNotificationService service = new BrevoCredentialsNotificationService(
                restClientBuilder,
                DISABLED,
                VALID_API_KEY,
                SENDER_EMAIL,
                SENDER_NAME
        );

        // Act
        service.sendWelcomeCredentials(USER_NAME, USER_EMAIL, TEMPORARY_PASSWORD, RESET_LINK);

        // Assert
        mockRestServiceServer.verify();
    }

    @Test
    void should_NotSendEmail_When_ApiKeyIsBlank() {
        // Arrange
        BrevoCredentialsNotificationService service = new BrevoCredentialsNotificationService(
                restClientBuilder,
                ENABLED,
                BLANK_API_KEY,
                SENDER_EMAIL,
                SENDER_NAME
        );

        // Act
        service.sendWelcomeCredentials(USER_NAME, USER_EMAIL, TEMPORARY_PASSWORD, RESET_LINK);

        // Assert
        mockRestServiceServer.verify();
    }

    @Test
    void should_SendEmail_When_BrevoIsEnabledAndApiKeyIsPresent() {
        // Arrange
        BrevoCredentialsNotificationService service = new BrevoCredentialsNotificationService(
                restClientBuilder,
                ENABLED,
                VALID_API_KEY,
                SENDER_EMAIL,
                SENDER_NAME
        );
        mockRestServiceServer.expect(requestTo(UsersConstants.BREVO_BASE_URL + UsersConstants.BREVO_EMAIL_PATH))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(UsersConstants.BREVO_API_KEY_HEADER, VALID_API_KEY))
                .andRespond(withStatus(HttpStatus.OK));

        // Act
        service.sendWelcomeCredentials(USER_NAME, USER_EMAIL, TEMPORARY_PASSWORD, RESET_LINK);

        // Assert
        mockRestServiceServer.verify();
    }

    @Test
    void should_ThrowExternalServiceException_When_BrevoRequestFails() {
        // Arrange
        BrevoCredentialsNotificationService service = new BrevoCredentialsNotificationService(
                restClientBuilder,
                ENABLED,
                VALID_API_KEY,
                SENDER_EMAIL,
                SENDER_NAME
        );
        mockRestServiceServer.expect(requestTo(UsersConstants.BREVO_BASE_URL + UsersConstants.BREVO_EMAIL_PATH))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        // Act
        ExternalServiceException exception = assertThrows(
                ExternalServiceException.class,
                () -> service.sendWelcomeCredentials(USER_NAME, USER_EMAIL, TEMPORARY_PASSWORD, RESET_LINK)
        );

        // Assert
        assertEquals("No fue posible enviar el correo de credenciales", exception.getMessage());
    }
}
