package com.shopcore.user.service.exception;

import org.springframework.http.HttpStatus;

public class UserAlreadyExistsException extends ServiceException {
    public UserAlreadyExistsException() {
        super("USER_ALREADY_EXISTS", HttpStatus.CONFLICT, "Username or email already exists");
    }
}
