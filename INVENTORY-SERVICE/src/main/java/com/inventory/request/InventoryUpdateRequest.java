package com.inventory.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InventoryUpdateRequest {
    private Long id;               // Inventory Master Id
    private Integer newQuantity;   // Updated total
    private Long updatedBy;
}

