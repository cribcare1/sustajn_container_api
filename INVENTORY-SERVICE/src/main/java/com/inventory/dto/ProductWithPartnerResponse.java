package com.inventory.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductWithPartnerResponse {
    private Integer containerTypeId;
    private String name;
    private String productId; // Maps to "ST-DC-50"
    private String capacity;  // Maps to "50ml"
    private String imageUrl;

    // The metric for this specific screen
    private Integer withPartnerCount;
}