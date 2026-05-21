package com.cinema.users_service.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class TemporaryPasswordService {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@#$%";
    private static final int LENGTH = 12;
    private final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        StringBuilder value = new StringBuilder(LENGTH);
        for (int index = 0; index < LENGTH; index++) {
            value.append(CHARS.charAt(secureRandom.nextInt(CHARS.length())));
        }
        return value.toString();
    }
}