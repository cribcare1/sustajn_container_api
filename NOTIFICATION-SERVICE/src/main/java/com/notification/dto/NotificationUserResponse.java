package com.notification.dto;

import com.notification.entity.AccountStatus;
import com.notification.entity.UserType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationUserResponse {
    private Long id;
    private UserType userType;
    private String fullName;
    private String email;
    private String phoneNumber;
    private Boolean pushNotification;
    private AccountStatus accountStatus;
}
