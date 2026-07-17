package com.sustajn.oderservice.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PopularProductInfo {
    private Long productId;
    private String name;
    private String productCode;
    private String capacity;
    private int percentage; // e.g., 68 for "68%"
}