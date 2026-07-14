package com.inventory.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantStockResponse {
    private Integer id;
    private String name;          // e.g., "Dip Cup"
    private String productCode;   // e.g., "ST-DC-50"
    private String capacity;      // e.g., "50ml"
    private String imageUrl;
    private Integer inStockCount; // Maps to "In-Stock" label in UI
}