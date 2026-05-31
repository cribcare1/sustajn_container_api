package com.auth.request;

import lombok.Data;

@Data
public class FeedbackRequest {
    private Long userId;

    private String rating;
    private String subject;
    private String remark;
}