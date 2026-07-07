package com.inventory.dto;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmedOrderDetailResponse {
    private Long id;
    private String orderId;
    private String restaurantName;
    private String restaurantAddress;
    private String partnerRemark;      // Maps to "Partner Remarks" section
    private String sustajnRemark;      // Maps to "Sustajn Remarks" section
    private String orderedOnDate;      // Returns format: "10.12.2025"
    private String orderedOnTime;      // Returns format: "10:00"
    private String confirmedOnDate;    // Returns format: "10.12.2025"
    private String confirmedOnTime;    // Returns format: "10:00"
    private List<ConfirmedItemDetail> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfirmedItemDetail {
        private Long itemId;
        private String containerName;   // e.g., "Dip Cup"
        private String productCode;     // e.g., "ST-DC-50"
        private String capacity;        // e.g., "50ml"
        private String imageUrl;
        private Integer orderedQty;     // Displays confirmed/approved quantity value
    }
}