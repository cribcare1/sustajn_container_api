package com.sustajn.oderservice.dto;

import lombok.*;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
public class ApiResponse<T> {

    private String status;
    private String message;
    private T data;

    public ApiResponse(String status, String message) {
        this.status = status;
        this.message = message;
    }

    public ApiResponse(String status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }


}
