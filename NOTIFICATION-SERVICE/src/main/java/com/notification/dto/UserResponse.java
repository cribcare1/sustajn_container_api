package com.notification.dto;

import com.notification.entity.AccountStatus;
import com.notification.entity.UserType;
import lombok.Data;

@Data
public class UserResponse {

    private Long id;
    private UserType userType;
    private String fullName;
    private String email;
    private String phoneNumber;
    private Boolean pushNotification;
    private AccountStatus accountStatus;

}
