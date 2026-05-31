package com.sustajn.oderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProductUsageTemp {
    private RestaurantContainerInventoryResponse inventory;
    private double percentage;
}
