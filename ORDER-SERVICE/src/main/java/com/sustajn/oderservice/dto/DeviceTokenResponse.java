package com.sustajn.oderservice.dto;


import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DeviceTokenResponse {

    private Long id;

    private Long userId;

    private String deviceToken;   // FCM token / APNS token

    private String deviceType;    // ANDROID / IOS / WEB

    private Boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
