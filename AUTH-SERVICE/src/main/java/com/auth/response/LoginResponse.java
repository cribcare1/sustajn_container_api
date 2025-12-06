package com.auth.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {
    private Long userId;
    private String image;
    private String role;
    private String userName;
    private String address;
    private String fullName;
    private String jwtToken;
    private String tokenType;
}
