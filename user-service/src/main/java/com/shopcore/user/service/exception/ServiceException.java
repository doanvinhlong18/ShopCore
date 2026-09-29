package com.shopcore.user.service.exception;

import org.springframework.http.HttpStatus;

public class ServiceException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public ServiceException(String code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
