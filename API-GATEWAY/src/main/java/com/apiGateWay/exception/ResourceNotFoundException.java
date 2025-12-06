package com.apiGateWay.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
public class ResourceNotFoundException extends RuntimeException {
    private String message;
    private final HttpStatus status;

    public ResourceNotFoundException(String message) {
        this.message=message;
        this.status = HttpStatus.NOT_FOUND;
    }

    public ResourceNotFoundException(String message, HttpStatus status) {
        this.message=message;
        this.status = status;
    }

}

