package com.auth.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class RestaurantFeedbackRequest {

    private Long restaurantId;
    private Integer rating;
    private String subject;
    private String remarks;
}
