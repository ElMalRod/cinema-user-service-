package com.cinema.users_service.exception;

public class InvalidHeaderException extends RuntimeException {

    public InvalidHeaderException(String message) {
        super(message);
    }
}