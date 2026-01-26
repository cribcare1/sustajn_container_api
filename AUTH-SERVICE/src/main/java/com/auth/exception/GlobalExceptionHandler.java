package com.auth.exception;

import com.auth.constant.AuthConstant;
import com.auth.response.ApiResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFoundException(ResourceNotFoundException exception) {
        ApiResponse<Object> response = ApiResponse.builder()
                .status(AuthConstant.ERROR)
                .message(exception.getMessage())
                .data(LocalDateTime.now())   // optional: include timestamp in `data`
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadRequestException(BadRequestException exception) {
        ApiResponse<Object> response = ApiResponse.builder()
                .status(AuthConstant.ERROR)
                .message(exception.getMessage())
                .data(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(GenericException.class)
    public ResponseEntity<ApiResponse<?>> handleGenericException(GenericException exception) {
        ApiResponse<Object> response = ApiResponse.builder()
                .status(AuthConstant.ERROR)
                .message(exception.getMessage())
                .data(null)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(Exception exception) {
        ApiResponse<Object> response = ApiResponse.builder()
                .status(AuthConstant.ERROR)
                .message(exception.getMessage())
                .data(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(MethodArgumentNotValidException ex) {

        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getDefaultMessage())
                .findFirst()
                .orElse("Validation error");

        ApiResponse<Object> response = ApiResponse.builder()
                .status(AuthConstant.ERROR)
                .message(errorMessage)
                .data(null)
                .build();
        return ResponseEntity.badRequest().body(response);
    }


}
