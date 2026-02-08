package com.sustajn.oderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RestaurantContainerInventoryResponse {

    private Long id;

    private Long restaurantId;

    private Integer containerTypeId;

    private String containerTypeName;

    private String productUniqueId;

    private String productImageUrl;

    private Integer capacity;

    private Integer currentQuantity;   // containers currently with restaurant

}
