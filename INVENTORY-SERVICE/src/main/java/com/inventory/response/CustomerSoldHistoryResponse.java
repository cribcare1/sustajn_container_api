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
public class CustomerSoldHistoryResponse {
    private String monthYear; // e.g. "November-2025"
    private List<CustomerSoldItemDto> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerSoldItemDto {
        private String productName;        // "Dip Cup"
        private String productUniqueId;    // "ST-DC-50"
        private String capacity;           // "50ml"
        private String imageUrl;
        private Integer soldQuantity;      // 2
        private Long totalAmount;          // 100
        private String borrowedOn;         // "17.11.2025"
        private String dueOn;              // "24.11.2025"
        private String soldOn;             // "25.11.2025"
    }
}