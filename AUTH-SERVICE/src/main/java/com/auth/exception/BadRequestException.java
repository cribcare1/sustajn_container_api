package com.auth.exception;

import com.auth.constant.AuthConstant;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
public class BadRequestException extends RuntimeException {
    private String message;
    private final String status;

    public BadRequestException(String message) {
        this.message=message;
        this.status = AuthConstant.ERROR;
    }

    public BadRequestException(String message, String status) {
        this.message=message;
        this.status = status;
    }}
