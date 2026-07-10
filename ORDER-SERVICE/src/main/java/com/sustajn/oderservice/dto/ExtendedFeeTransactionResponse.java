package com.sustajn.oderservice.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtendedFeeTransactionResponse {
    private Long orderId;
    private String name;                       // Displays "JACK-1234" or "Couqley French Brasserie"
    private String formattedDateTime;          // Displays "21.11.2025 | 10:00"
    private Integer totalQuantity;             // Total item count extended
    private BigDecimal totalAmount;            // Total fee charged for this order extension
}