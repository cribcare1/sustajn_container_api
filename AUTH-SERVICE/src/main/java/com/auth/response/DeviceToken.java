package com.auth.response;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeviceToken {


    private Long id;

    private Long userId;

    private String deviceToken;   // FCM token / APNS token

    private String deviceType;    // ANDROID / IOS / WEB

    private Boolean active = true;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
