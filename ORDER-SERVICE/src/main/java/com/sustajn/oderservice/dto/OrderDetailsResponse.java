package com.sustajn.oderservice.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderDetailsResponse {
    private Integer restaurantId;
    private String restaurantName;
    private String restaurantAddress;
    private Long productId;
    private String productName;
    private Integer quantity;
}
