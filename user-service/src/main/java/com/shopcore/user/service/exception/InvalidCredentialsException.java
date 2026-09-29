package com.shopcore.user.service.exception;

import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends ServiceException {
    public InvalidCredentialsException() {
        super("AUTH_INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }
}
