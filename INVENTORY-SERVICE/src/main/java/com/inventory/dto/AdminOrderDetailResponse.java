package com.inventory.dto;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminOrderDetailResponse {

    private Long id;
    private String orderId;             // Displays "#ORD-00234"
    private String restaurantName;      // Resolved from Auth-Service
    private String restaurantAddress;
    private String restaurantRemark;    // Displays inside "Partner Remarks" section
    private String orderType;           // "BORROW" or "RETURN"
    private String orderedOnDate;       // Format: "27.11.2025"
    private String orderedOnTime;       // Format: "11:00"
    private List<ItemDetail> items;     // Maps the card rows scroll view list

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemDetail {
        private Long itemId;            // AdminOrderItem primary key ID
        private Integer containerTypeId;// Underbuilt database record ID reference
        private String containerName;   // Displays "Dip Cups", "Round Container"
        private String productCode;     // Displays "ST-DC-50", "ST-RDC-500"
        private String capacity;        // Displays "50ml", "500ml"
        private String imageUrl;        // Thumbnail asset resource locator url string
        private Integer orderedQty;     // Displays under "Ordered Qty." label
        private Integer availableQty;   // Displays under "Available Qty." label
    }
}