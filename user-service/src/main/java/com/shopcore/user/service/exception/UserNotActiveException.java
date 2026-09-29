package com.shopcore.user.service.exception;

import org.springframework.http.HttpStatus;

public class UserNotActiveException extends ServiceException {
    public UserNotActiveException() {
        super("USER_NOT_ACTIVE", HttpStatus.FORBIDDEN, "User account is not active");
    }
}
