package com.inventory.dto;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveredOrderDetailResponse {
    private Long id;
    private String orderId;
    private String restaurantName;
    private String restaurantAddress;  // e.g., "Pier 7, 2nd Floor, Dubai Marina, Dubai"

    // Three-Step Milestone Timeline Fields
    private String orderedDate;        // e.g., "20.11.2025"
    private String orderedTime;        // e.g., "10:00"
    private String confirmedDate;      // e.g., "20.11.2025"
    private String confirmedTime;      // e.g., "11:00"
    private String deliveredDate;      // e.g., "27.11.2025"
    private String deliveredTime;      // e.g., "13:00"

    private List<DeliveredItemDetail> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeliveredItemDetail {
        private Long itemId;
        private String containerName;   // e.g., "Dip Cups"
        private String productCode;     // e.g., "ST-DC-50"
        private String capacity;        // e.g., "50ml"
        private String imageUrl;
        private Integer deliveredQty;   // Displays under "Delivered Containers" slot
    }
}