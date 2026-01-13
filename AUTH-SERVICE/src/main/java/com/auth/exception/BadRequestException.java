package com.auth.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
public class BadRequestException extends RuntimeException {
    private String message;
    private final HttpStatus status;

    public BadRequestException(String message) {
        this.message=message;
        this.status = HttpStatus.BAD_REQUEST;
    }

    public BadRequestException(String message, HttpStatus status) {
        this.message=message;
        this.status = status;
    }}
