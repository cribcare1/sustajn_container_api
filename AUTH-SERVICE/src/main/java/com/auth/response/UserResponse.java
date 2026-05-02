package com.auth.response;

import lombok.*;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserResponse {

    private Long id;
    private String fullName;
    private String customerId;
    private String phoneNumber;


}
