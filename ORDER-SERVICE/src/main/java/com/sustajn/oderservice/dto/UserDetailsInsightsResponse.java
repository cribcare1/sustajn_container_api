package com.sustajn.oderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailsInsightsResponse {

    private Integer activeCount;
    private Integer overdueCount;
    private UserProductStat mostBorrowed;
    private UserProductStat lessBorrowed;
    private UserProductStat mostReturn;
    private UserProductStat lessReturn;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserProductStat {
        private Long productId;
        private String productName;
        private String productCode;  // e.g., "ST-RDC-500"
        private String capacity;     // e.g., "500ml"
        private String imageUrl;
        private int percentage;      // Relative share percentage
    }
}