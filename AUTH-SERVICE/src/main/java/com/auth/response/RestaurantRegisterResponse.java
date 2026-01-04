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
    private String address;

    public RestaurantRegisterResponse(Long restaurantId, String name, String email, String phoneNumber, String profileImageUrl) {
        this.restaurantId = restaurantId;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.profileImageUrl = profileImageUrl;
    }
}
