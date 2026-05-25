package com.cinema.users_service.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private static final String REQUEST_PATH = "/users/profile";
    private static final String FIELD_NAME = "name";
    private static final String DEFAULT_VALIDATION_MESSAGE = "must not be blank";
    private static final String GENERIC_MESSAGE = "generic";
    private static final String USER_NOT_FOUND_MESSAGE = "Perfil de usuario no encontrado";
    private static final String INSUFFICIENT_BALANCE_MESSAGE = "Saldo insuficiente";
    private static final String BAD_REQUEST_MESSAGE = "El monto debe ser mayor a cero";
    private static final String FORBIDDEN_MESSAGE = "Acceso denegado";
    private static final String CONFLICT_MESSAGE = "El email ya existe en auth-service";
    private static final String EXTERNAL_MESSAGE = "No fue posible conectar";
    private static final String INTERNAL_SERVER_MESSAGE = "Error interno del servidor";

    @Mock
    private HttpServletRequest httpServletRequest;

    private GlobalExceptionHandler globalExceptionHandler;

    @BeforeEach
    void setUp() {
        globalExceptionHandler = new GlobalExceptionHandler();
        when(httpServletRequest.getRequestURI()).thenReturn(REQUEST_PATH);
    }

    @Test
    void should_ReturnNotFound_When_UserProfileNotFoundExceptionIsHandled() {
        // Arrange
        UserProfileNotFoundException exception = new UserProfileNotFoundException(USER_NOT_FOUND_MESSAGE);

        // Act
        var response = globalExceptionHandler.handleProfileNotFound(exception, httpServletRequest);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND.value(), response.getBody().status());
        assertEquals(USER_NOT_FOUND_MESSAGE, response.getBody().message());
    }

    @Test
    void should_ReturnBadRequest_When_InsufficientBalanceExceptionIsHandled() {
        // Arrange
        InsufficientBalanceException exception = new InsufficientBalanceException(INSUFFICIENT_BALANCE_MESSAGE);

        // Act
        var response = globalExceptionHandler.handleInsufficient(exception, httpServletRequest);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().status());
        assertEquals(INSUFFICIENT_BALANCE_MESSAGE, response.getBody().message());
    }

    @Test
    void should_ReturnBadRequest_When_RuntimeBadRequestExceptionIsHandled() {
        // Arrange
        InvalidAmountException exception = new InvalidAmountException(BAD_REQUEST_MESSAGE);

        // Act
        var response = globalExceptionHandler.handleBadRequest(exception, httpServletRequest);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().status());
        assertEquals(BAD_REQUEST_MESSAGE, response.getBody().message());
    }

    @Test
    void should_ReturnForbidden_When_ForbiddenAccessExceptionIsHandled() {
        // Arrange
        ForbiddenAccessException exception = new ForbiddenAccessException(FORBIDDEN_MESSAGE);

        // Act
        var response = globalExceptionHandler.handleForbidden(exception, httpServletRequest);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN.value(), response.getBody().status());
        assertEquals(FORBIDDEN_MESSAGE, response.getBody().message());
    }

    @Test
    void should_ReturnConflict_When_DuplicateUserExceptionIsHandled() {
        // Arrange
        DuplicateUserException exception = new DuplicateUserException(CONFLICT_MESSAGE);

        // Act
        var response = globalExceptionHandler.handleConflict(exception, httpServletRequest);

        // Assert
        assertEquals(HttpStatus.CONFLICT.value(), response.getBody().status());
        assertEquals(CONFLICT_MESSAGE, response.getBody().message());
    }

    @Test
    void should_ReturnBadGateway_When_ExternalServiceExceptionIsHandled() {
        // Arrange
        ExternalServiceException exception = new ExternalServiceException(EXTERNAL_MESSAGE);

        // Act
        var response = globalExceptionHandler.handleExternal(exception, httpServletRequest);

        // Assert
        assertEquals(HttpStatus.BAD_GATEWAY.value(), response.getBody().status());
        assertEquals(EXTERNAL_MESSAGE, response.getBody().message());
    }

    @Test
    void should_ReturnValidationMessage_When_MethodArgumentNotValidExceptionContainsFieldError() throws Exception {
        // Arrange
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", FIELD_NAME, DEFAULT_VALIDATION_MESSAGE));
        Method method = ValidationFixture.class.getDeclaredMethod("validate", String.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(parameter, bindingResult);

        // Act
        var response = globalExceptionHandler.handleValidation(exception, httpServletRequest);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().status());
        assertEquals(FIELD_NAME + " " + DEFAULT_VALIDATION_MESSAGE, response.getBody().message());
    }

    @Test
    void should_ReturnInternalServerError_When_GenericExceptionIsHandled() {
        // Arrange
        Exception exception = new Exception(GENERIC_MESSAGE);

        // Act
        var response = globalExceptionHandler.handleGeneric(exception, httpServletRequest);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getBody().status());
        assertEquals(INTERNAL_SERVER_MESSAGE, response.getBody().message());
        assertEquals(REQUEST_PATH, response.getBody().path());
    }

    private static class ValidationFixture {
        @SuppressWarnings("unused")
        private void validate(String value) {
        }
    }
}
