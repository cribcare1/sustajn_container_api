package com.notification.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyRequest {
    private String email;
    private String token;
    private String newPassword;
}
