package com.auth.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestaurantRegisterResponse {

    private Long restaurantId;
    private String name;
    private String email;
    private String phoneNumber;
    private String profileImageUrl;
}
