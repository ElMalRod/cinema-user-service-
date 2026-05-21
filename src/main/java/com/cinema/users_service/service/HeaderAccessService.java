package com.cinema.users_service.service;

import com.cinema.users_service.constants.UsersConstants;
import com.cinema.users_service.exception.ForbiddenAccessException;
import com.cinema.users_service.exception.InvalidHeaderException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class HeaderAccessService {

    public UUID parseUserId(String userIdHeader) {
        try {
            return UUID.fromString(userIdHeader);
        } catch (Exception exception) {
            throw new InvalidHeaderException("Header X-User-Id invalido");
        }
    }

    public void requireSystemAdmin(String roleHeader) {
        if (!UsersConstants.ROLE_SYSTEM_ADMIN.equalsIgnoreCase(String.valueOf(roleHeader))) {
            throw new ForbiddenAccessException("Solo SYSTEM_ADMIN puede ejecutar esta accion");
        }
    }

    public void requireInternalService(String internalHeader) {
        if (!UsersConstants.HEADER_INTERNAL_SERVICE_VALUE.equalsIgnoreCase(String.valueOf(internalHeader))) {
            throw new ForbiddenAccessException("Acceso interno requerido");
        }
    }
}