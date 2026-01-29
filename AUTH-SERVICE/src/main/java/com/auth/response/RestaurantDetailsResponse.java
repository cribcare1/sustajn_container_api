package com.auth.response;

import com.auth.model.BasicRestaurantDetails;
import com.auth.model.SocialMediaDetails;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RestaurantDetailsResponse {

    private Long id;
    private String fullName;
    private String mobileNumber;
    private String secondaryNumber;
    private Integer planId;
    private String subscriptionType;
    private BasicRestaurantDetails basicRestaurantDetails;
    private ContactAndRegistrationDetailsResponse contactAndRegistrationDetailsResponse;
    private List<SocialMediaDetails> socialMediaDetailsList;
}
