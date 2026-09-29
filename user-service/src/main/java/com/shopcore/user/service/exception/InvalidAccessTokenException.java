package com.shopcore.user.service.exception;

import org.springframework.http.HttpStatus;

public class InvalidAccessTokenException extends ServiceException {
    public InvalidAccessTokenException() {
        super("AUTH_TOKEN_INVALID", HttpStatus.UNAUTHORIZED, "Access token is invalid");
    }
}
