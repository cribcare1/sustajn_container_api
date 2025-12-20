package com.auth.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProfileResponse {
    private Long id;
    private String fullName;
    private String email;
    private String address;
    private String phoneNumber;
    private String profilePictureUrl;
}