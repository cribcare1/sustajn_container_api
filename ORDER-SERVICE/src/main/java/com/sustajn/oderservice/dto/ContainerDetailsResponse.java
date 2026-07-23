package com.sustajn.oderservice.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContainerDetailsResponse {

    private Long productId;
    private String name;
    private String productCode;
    private String capacity;
    private String imageUrl;

    private int orderedCount;
    private int issuedToPartnerCount;
    private int inCirculationCount;
    private int withPartnerCount;
    private int soldCount;
    private int damagedCount;
    private int inStockCount;
    private int returnedCount;
}