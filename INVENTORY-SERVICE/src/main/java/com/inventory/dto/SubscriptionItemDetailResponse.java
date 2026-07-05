package com.inventory.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionItemDetailResponse {
    private Long id;
    private String name;
    private String userType;
    private String restaurantAddress;
    private String planType;
    private BigDecimal amount;
    private String formattedDate;
}