package com.shopcore.user.service.exception;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends ServiceException {
    public UserNotFoundException() {
        super("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "User not found");
    }
}
