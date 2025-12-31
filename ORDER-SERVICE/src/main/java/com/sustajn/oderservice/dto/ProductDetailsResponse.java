package com.sustajn.oderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProductDetailsResponse {
    private Long orderId;
    private Long productId;
    private String productName;
    private int quantity;
    private String productImageUrl;
    private Long daysLeft;
    private String productUniqueId;
    private Integer containerQuantity;
}
