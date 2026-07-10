package com.sustajn.oderservice.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserExtendedFeeItemResponse {
    private Long orderId;
    private String formattedDateTime;          // Displays "21.11.2025 | 10:23"
    private Integer totalQuantity;             // Sum of items extended inside this order
    private BigDecimal totalAmount;            // Fee amount charged
}