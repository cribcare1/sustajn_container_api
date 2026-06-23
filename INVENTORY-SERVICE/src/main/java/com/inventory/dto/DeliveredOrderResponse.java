package com.inventory.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveredOrderResponse {
    private Long id;
    private String requestNumber;      // Maps to orderId (e.g., "#ORD-00232")
    private String requestType;        // "BORROW" or "RETURN"
    private String restaurantName;     // e.g., "Cloud (Healthy Wraps)"
    private String containerCodes;     // e.g., "ST-DC-50"
    private String formattedDateTime;  // e.g., "27.11.2025 | 11:00"
    private Integer totalQuantity;     // Sum of completed items delivered
}