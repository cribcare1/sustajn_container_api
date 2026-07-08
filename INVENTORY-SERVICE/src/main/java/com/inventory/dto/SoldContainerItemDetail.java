package com.inventory.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SoldContainerItemDetail {
    private Integer containerTypeId;
    private String containerName;             // Displays e.g., "Dip Cups"
    private String productCode;               // Displays e.g., "ST-DC-50"
    private String capacity;                  // Displays e.g., "50ml"
    private String imageUrl;
    private Integer quantity;                  // Individual item count
    private Integer price;                     // Cost charged for this container subgroup
}