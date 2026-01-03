package com.auth.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class FeedbackResponse {
    private Long id;
    private String customerName;          // RENAMED

    private Long restaurantId;
    private String rating;
    private String subject;
    private String remark;
    private LocalDateTime createdAt;
}