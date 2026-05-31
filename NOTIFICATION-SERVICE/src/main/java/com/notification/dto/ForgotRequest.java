package com.notification.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ForgotRequest {
    @NotNull(message = "Email cannot be null")
    private String email;
    @NotNull(message = "Type cannot be null")
    private String type;
}
