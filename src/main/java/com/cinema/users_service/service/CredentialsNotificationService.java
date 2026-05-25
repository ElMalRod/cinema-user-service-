package com.cinema.users_service.service;

public interface CredentialsNotificationService {

    void sendWelcomeCredentials(String name, String email, String temporaryPassword, String resetLink);
}