package com.auth.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RestaurantListResponse {

    private Long id;
    private String name;
    private String profileImageUrl;
    private String address;
}
