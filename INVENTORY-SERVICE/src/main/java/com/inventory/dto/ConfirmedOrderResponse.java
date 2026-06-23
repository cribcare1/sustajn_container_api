package com.inventory.dto;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmedOrderResponse {
    private Long id;
    private String requestNumber;      // Displays "#ORD-00234" or "#ORD-00233"
    private String requestType;        // "BORROW" or "RETURN"
    private String restaurantName;     // e.g., "ROKA Business Bay"
    private String containerCodes;     // e.g., "ST-DC-50 | ST-RDC-500"
    private String formattedDateTime;  // e.g., "10.11.2025 | 11:00"
    private Integer totalQuantity;     // Sum of all confirmed container units
}