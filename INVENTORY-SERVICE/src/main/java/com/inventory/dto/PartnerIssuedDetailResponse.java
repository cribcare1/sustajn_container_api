package com.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerIssuedDetailResponse {
    private Long orderId;
    private Long restaurantId;
    private String restaurantName;      // e.g., "Kimura-ya Authentic Japanese Restaurant"
    private String restaurantAddress;   // e.g., "The Oberoi Hotel, Al A'amal Street..."
    private String containerCode;       // e.g., "ST-DC-50"
    private Integer quantity;           // e.g., 350
    private String orderedDate;         // e.g., "24.11.2025"
    private String deliveredDate;       // e.g., "25.11.2025"
}