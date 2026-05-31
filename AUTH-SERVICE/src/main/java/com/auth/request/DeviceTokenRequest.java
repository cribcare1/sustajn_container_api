package com.auth.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeviceTokenRequest {
    private Long userId;
    private String deviceToken;
    private String deviceType;
}