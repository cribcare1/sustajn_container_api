package com.auth.response;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerSubscriptionInternalResponse {
    private Long userId;
    private String name;               // Full Name or Restaurant Name
    private String userType;           // "CUSTOMER" or "PARTNER"/"RESTAURANT"
    private Integer subscriptionPlanId;
    private String concatenatedAddress;
    private LocalDateTime trackedAt;
}