package com.auth.request;

import lombok.Data;

@Data
public class FeedbackRequest {
    private Long customerId;    // RENAMED
    private Long restaurantId;
    private String rating;
    private String subject;
    private String remark;
}