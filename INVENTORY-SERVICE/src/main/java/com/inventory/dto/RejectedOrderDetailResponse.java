package com.inventory.dto;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RejectedOrderDetailResponse {
    private Long id;
    private String orderId;
    private String restaurantName;
    private String restaurantAddress;  // e.g., "Pier 7, 2nd Floor, Dubai Marina..."

    // Timeline tracking properties
    private String orderedDate;        // e.g., "20.11.2025"
    private String orderedTime;        // e.g., "10:00"
    private String rejectedDate;       // e.g., "27.11.2025"
    private String rejectedTime;       // e.g., "13:00"
    private String rejectedRemark;      // Maps to "Rejected Remarks" section

    private List<RejectedItemDetail> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RejectedItemDetail {
        private Long itemId;
        private String containerName;   // e.g., "Dip Cup"
        private String productCode;     // e.g., "ST-DC-50"
        private String capacity;        // e.g., "50ml"
        private String imageUrl;
        private Integer requestedQty;   // Displays the original quantity they asked for
    }
}