package com.inventory.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private Map<String, Object> buildResponse(String message, String status, Object data) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("message", message);
        resp.put("status", status);
        resp.put("data", data);
        return resp;
    }

    @ExceptionHandler(FileStorageException.class)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleFileStorageException(FileStorageException ex) {
        log.error("FileStorageException: {}", ex.getMessage(), ex);
        return new ResponseEntity<>(buildResponse(ex.getMessage(), "file_error", null), HttpStatus.INTERNAL_SERVER_ERROR);
    }

//    @ExceptionHandler(MethodArgumentNotValidException.class)
//    @ResponseBody
//    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
//        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
//                .map(fe -> {
//                    Map<String, String> m = new HashMap<>();
//                    m.put("field", fe.getField());
//                    m.put("message", fe.getDefaultMessage());
//                    return m;
//                })
//                .collect(Collectors.toList());
//        log.warn("Validation failed: {}", errors);
//        return new ResponseEntity<>(buildResponse("Validation failed", "error", errors), HttpStatus.BAD_REQUEST);
//    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        log.warn("Upload too large: {}", ex.getMessage());
        return new ResponseEntity<>(buildResponse("Validation failed", "error", ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleBadRequest(HttpMessageNotReadableException ex) {
        log.warn("Malformed JSON request: {}", ex.getMessage());
        return new ResponseEntity<>(buildResponse("Malformed request body", "error", null), HttpStatus.OK);
    }

    @ExceptionHandler(InventoryException.class)
    public ResponseEntity<?> handleInventoryException(InventoryException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("status", "error");
        error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.OK).body(error);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<?> handleBusinessException(BusinessException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("status", "error");
        error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.OK).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return new ResponseEntity<>(buildResponse(ex.getMessage(), "error", null), HttpStatus.OK);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return new ResponseEntity<>(buildResponse(ex.getMessage(), "error", null), HttpStatus.OK);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleDuplicateResourceException(DuplicateResourceException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return new ResponseEntity<>(buildResponse(ex.getMessage(), "error", null), HttpStatus.OK);
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleAll(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return new ResponseEntity<>(buildResponse("Internal server error" + ex.getMessage(), "error", null), HttpStatus.OK);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Validation failed");
        response.put("message", ex.getMessage()); // or customize further
        response.put("status", HttpStatus.BAD_REQUEST.value());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Invalid argument");
        response.put("message", ex.getBindingResult().getFieldError().getDefaultMessage());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingPathVariableException.class)
    public ResponseEntity<Map<String, Object>> handleMissingPathVariable(MissingPathVariableException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Missing path variable");
        response.put("message", ex.getVariableName() + " must not be null");
        response.put("status", HttpStatus.BAD_REQUEST.value());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

//    @ExceptionHandler(HandlerMethodValidationException.class)
//    public ResponseEntity<Map<String, Object>> handleHandlerMethodValidationException(HandlerMethodValidationException ex) {
//        Map<String, Object> response = new HashMap<>();
//        response.put("error", "Validation failed");
//        String message = ex.getAllValidationResults().stream().flatMap(result -> result.getResolvableErrors().stream()).map(error -> error.getDefaultMessage()).findFirst().orElse("Invalid input");
//        response.put("message", message);
//        response.put("status", HttpStatus.BAD_REQUEST.value());
//        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
//    }
}

