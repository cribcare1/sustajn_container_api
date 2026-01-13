package com.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceResponse<T> {
    private String message;
    private String status;
    private T data;

    public static <T> ServiceResponse<T> success(String message, T data) {
        return new ServiceResponse<>(message, "success", data);
    }

    public static <T> ServiceResponse<T> failure(String message, String status) {
        return new ServiceResponse<>(message, status, null);
    }
}
