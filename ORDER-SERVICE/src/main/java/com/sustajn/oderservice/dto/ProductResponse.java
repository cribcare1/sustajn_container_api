package com.sustajn.oderservice.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ProductResponse {
    private Integer productId;
    private String productName;
    private String productDescription;
    private Double price;
    private String productImageUrl;
    private Integer capacity;
    private String productUniqueId;
}
