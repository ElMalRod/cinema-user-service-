package com.cinema.users_service.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class TemporaryPasswordServiceTest {

    private static final int PASSWORD_LENGTH = 12;
    private static final String ALLOWED_CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@#$%";

    @Test
    void should_GeneratePasswordWithExpectedLengthAndAllowedCharacters_When_GenerateIsCalled() {
        // Arrange
        TemporaryPasswordService temporaryPasswordService = new TemporaryPasswordService();

        // Act
        String generatedPassword = temporaryPasswordService.generate();

        // Assert
        assertEquals(PASSWORD_LENGTH, generatedPassword.length());
        assertTrue(generatedPassword.chars().allMatch(character -> ALLOWED_CHARACTERS.indexOf(character) >= 0));
    }
}
