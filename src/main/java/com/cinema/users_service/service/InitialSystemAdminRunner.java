package com.cinema.users_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InitialSystemAdminRunner implements ApplicationRunner {

    private final AdminUserService adminUserService;
    private final int maxAttempts;
    private final long retryDelayMs;

    public InitialSystemAdminRunner(
            AdminUserService adminUserService,
            @Value("${users.bootstrap.max-attempts:20}") int maxAttempts,
            @Value("${users.bootstrap.retry-delay-ms:3000}") long retryDelayMs
    ) {
        this.adminUserService = adminUserService;
        this.maxAttempts = maxAttempts;
        this.retryDelayMs = retryDelayMs;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                adminUserService.ensureInitialSystemAdmin();
                log.info("Initial SYSTEM_ADMIN bootstrap completed on attempt {}/{}", attempt, maxAttempts);
                return;
            } catch (Exception exception) {
                log.warn("Initial SYSTEM_ADMIN bootstrap attempt {}/{} failed: {}", attempt, maxAttempts, exception.getMessage());
                if (attempt == maxAttempts) {
                    throw new IllegalStateException("No fue posible garantizar el SYSTEM_ADMIN inicial", exception);
                }
                sleepBeforeRetry();
            }
        }
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(retryDelayMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Bootstrap interrumpido mientras esperaba reintento", exception);
        }
    }
}
