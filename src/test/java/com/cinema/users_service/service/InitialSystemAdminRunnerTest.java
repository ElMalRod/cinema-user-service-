package com.cinema.users_service.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InitialSystemAdminRunnerTest {

    private static final int SINGLE_ATTEMPT = 1;
    private static final int THREE_ATTEMPTS = 3;
    private static final int TWO_ATTEMPTS = 2;
    private static final long NO_DELAY_MS = 0L;
    private static final long RETRY_DELAY_MS = 1L;
    private static final String FAILURE_MESSAGE = "bootstrap-failure";
    private static final String[] EMPTY_ARGS = new String[0];

    @Mock
    private AdminUserService adminUserService;

    @AfterEach
    void clearInterruptedFlag() {
        Thread.interrupted();
    }

    @Test
    void should_CompleteOnFirstAttempt_When_BootstrapSucceeds() throws Exception {
        // Arrange
        InitialSystemAdminRunner runner = new InitialSystemAdminRunner(adminUserService, SINGLE_ATTEMPT, NO_DELAY_MS);

        // Act
        runner.run(new DefaultApplicationArguments(EMPTY_ARGS));

        // Assert
        verify(adminUserService, times(1)).ensureInitialSystemAdmin();
    }

    @Test
    void should_RetryUntilSuccess_When_InitialAttemptsFail() throws Exception {
        // Arrange
        doThrow(new RuntimeException(FAILURE_MESSAGE))
                .doThrow(new RuntimeException(FAILURE_MESSAGE))
                .doNothing()
                .when(adminUserService).ensureInitialSystemAdmin();
        InitialSystemAdminRunner runner = new InitialSystemAdminRunner(adminUserService, THREE_ATTEMPTS, NO_DELAY_MS);

        // Act
        runner.run(new DefaultApplicationArguments(EMPTY_ARGS));

        // Assert
        verify(adminUserService, times(THREE_ATTEMPTS)).ensureInitialSystemAdmin();
    }

    @Test
    void should_ThrowIllegalStateException_When_AllAttemptsFail() {
        // Arrange
        doThrow(new RuntimeException(FAILURE_MESSAGE))
                .when(adminUserService).ensureInitialSystemAdmin();
        InitialSystemAdminRunner runner = new InitialSystemAdminRunner(adminUserService, TWO_ATTEMPTS, NO_DELAY_MS);

        // Act
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> runner.run(new DefaultApplicationArguments(EMPTY_ARGS))
        );

        // Assert
        assertEquals("No fue posible garantizar el SYSTEM_ADMIN inicial", exception.getMessage());
        verify(adminUserService, times(TWO_ATTEMPTS)).ensureInitialSystemAdmin();
    }

    @Test
    void should_ThrowIllegalStateException_When_ThreadIsInterruptedDuringRetrySleep() {
        // Arrange
        doThrow(new RuntimeException(FAILURE_MESSAGE))
                .when(adminUserService).ensureInitialSystemAdmin();
        InitialSystemAdminRunner runner = new InitialSystemAdminRunner(adminUserService, TWO_ATTEMPTS, RETRY_DELAY_MS);
        Thread.currentThread().interrupt();

        // Act
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> runner.run(new DefaultApplicationArguments(EMPTY_ARGS))
        );

        // Assert
        assertEquals("Bootstrap interrumpido mientras esperaba reintento", exception.getMessage());
    }
}
