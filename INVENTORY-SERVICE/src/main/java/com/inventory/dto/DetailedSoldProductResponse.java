package com.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DetailedSoldProductResponse {
    private Integer productId;
    private String productName;
    private String productDescription;
    private String productImageUrl;
    private Integer capacity;
    private String productUniqueId;
    private Long soldAmount;
    private Integer soldQuantity;

    // The 3 Real Dates
    private String borrowedOn;
    private String dueOn;
    private String soldOn;
}