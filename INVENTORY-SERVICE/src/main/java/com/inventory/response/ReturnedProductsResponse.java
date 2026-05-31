package com.inventory.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReturnedProductsResponse {
    private Integer id;
    private String productName;
    private String productId; // e.g., "ST-DC-50"
    private Integer capacityMl;
    private Integer totalReturnedQuantity; // Sum of all returned items
}