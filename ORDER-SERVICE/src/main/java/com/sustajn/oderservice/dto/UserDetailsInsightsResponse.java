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
    @Builder // 🟢 Ensures builder works for the inner class
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserProductStat {
        private Long productId;
        private String productName;
        private String productCode;
        private String capacity;
        private String imageUrl;
        private int percentage;
    }
}