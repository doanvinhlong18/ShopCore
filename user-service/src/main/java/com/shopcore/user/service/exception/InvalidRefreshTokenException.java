package com.shopcore.user.service.exception;

import org.springframework.http.HttpStatus;

public class InvalidRefreshTokenException extends ServiceException {
    public InvalidRefreshTokenException() {
        super("AUTH_REFRESH_TOKEN_INVALID", HttpStatus.UNAUTHORIZED, "Invalid refresh token");
    }
}
