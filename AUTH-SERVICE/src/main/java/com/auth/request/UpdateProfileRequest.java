package com.auth.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequest {

    private String fullName;
    private String address;
    private String phoneNumber;
    private String profilePictureUrl;
}