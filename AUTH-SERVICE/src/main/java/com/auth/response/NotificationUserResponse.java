package com.auth.response;

import com.auth.enumDetails.AccountStatus;
import com.auth.enumDetails.UserType;
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
