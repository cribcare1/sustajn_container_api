package com.notification.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ForgotRequest {
    private String email;
    private String type;
}
