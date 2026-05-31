package com.inventory.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class IssuedProductsResponse {

    private Integer id;
    private String productName;
    private String productId;
    private Integer capacityMl;
    private Integer totalIssuedQuantity; // sum of approved quantities to the restaurant till date

}
