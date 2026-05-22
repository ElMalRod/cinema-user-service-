package com.cinema.users_service.exception;

public class InvalidAdminOperationException extends RuntimeException {

    public InvalidAdminOperationException(String message) {
        super(message);
    }
}
