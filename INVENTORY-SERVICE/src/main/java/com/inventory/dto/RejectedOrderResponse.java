package com.inventory.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RejectedOrderResponse {
    private Long id;
    private String requestNumber;      // Displays "#ORD-00123" or "#ORD-00312"
    private String requestType;        // "BORROW" or "RETURN"
    private String restaurantName;     // e.g., "Mama Zonia"
    private String containerCodes;     // e.g., "ST-DC-50" or "ST-DC-50 | ST-RDC-500"
    private String formattedDateTime;  // e.g., "27.11.2025 | 11:00"
    private Integer totalQuantity;     // Original requested quantity sum
}