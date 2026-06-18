package com.sustajn.oderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MostAndLeastLeasedResponse {

    private ProductLeasedStat mostLeased;
    private ProductLeasedStat lessLeased;

    @Data
    @Builder // 🟢 Ensures builder works for the inner class
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductLeasedStat {
        private Long productId;
        private String productName;
        private String productCode;
        private String capacity;
        private String imageUrl;
        private int percentage;
    }
}