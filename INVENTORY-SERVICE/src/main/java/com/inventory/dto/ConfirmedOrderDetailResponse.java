package com.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private String partnerRemark;
    private String sustajnRemark;

    // Standardized date & time fields
    private String orderDate;
    private String orderTime;
    private String orderedOnDate;
    private String orderedOnTime;
    private String confirmedOnDate;
    private String confirmedOnTime;

    private List<ConfirmedItemDetail> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfirmedItemDetail {
        private Long itemId;
        private String containerName;
        private String productCode;
        private String capacity;
        private String imageUrl;
        private Integer orderedQty;   // Quantity requested by partner
        private Integer approvedQty;  // Quantity approved by admin
    }
}