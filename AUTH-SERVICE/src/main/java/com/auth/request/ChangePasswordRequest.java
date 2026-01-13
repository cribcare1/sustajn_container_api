package com.auth.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordRequest {
    private Long userId;
    private String newPassword;
}
