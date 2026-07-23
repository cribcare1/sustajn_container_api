package com.inventory.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContainerInventoryStatsDto {

    private Integer containerTypeId;
    private String name;
    private String productCode;
    private String capacity;
    private String imageUrl;

    private int orderedCount;
    private int issuedToPartnerCount;
    private int withPartnerCount;
    private int soldCount;
    private int damagedCount;
    private int inStockCount;
}