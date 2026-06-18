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
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductLeasedStat {
        private Long productId;
        private String productName;
        private String productCode;  // Displays "ST-RDC-500"
        private String capacity;     // Displays "500ml"
        private String imageUrl;
        private int percentage;      // Calculated breakdown metric
    }
}