package com.inventory.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ContainerDetailsResponse {
    // Header Info
    private Integer containerTypeId;
    private String name;
    private String productId;
    private String capacity;
    private String imageUrl;

    // Grid Metrics
    private Integer ordered;
    private Integer issuedToPartner;
    private Integer inCirculation;
    private Integer withPartner;
    private Integer sold;
    private Integer damaged;
    private Integer inStock;
    private Integer returned;
}