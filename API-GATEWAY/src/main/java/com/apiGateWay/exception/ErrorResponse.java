package com.apiGateWay.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Getter
@Setter

public class ErrorResponse {
    private String message;
    private HttpStatus status;
    private LocalDateTime timeStamp = LocalDateTime.now();
}