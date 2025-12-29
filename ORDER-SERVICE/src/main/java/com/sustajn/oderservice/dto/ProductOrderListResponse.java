package com.sustajn.oderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProductOrderListResponse {
    private Integer productId;
    private String productName;
    private Integer capacity;
    private int containerCount;   // 👈 new field
    private String productImageUrl;
}
