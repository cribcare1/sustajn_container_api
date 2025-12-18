package com.inventory.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryWithContainerResponse {

    private Long inventoryId;

    private Integer containerTypeId;
    private String containerName;
    private String containerDescription;
    private Integer capacityMl;
    private String material;
    private String colour;

    private BigDecimal lengthCm;
    private BigDecimal widthCm;
    private BigDecimal heightCm;

    private Integer weightGrams;
    private Boolean foodSafe;
    private Boolean dishwasherSafe;
    private Boolean microwaveSafe;
    private Integer maxTemperature;
    private Integer minTemperature;
    private Integer lifespanCycle;

    private String imageUrl;
    private BigDecimal costPerUnit;

    private Integer totalContainers;
    private Integer availableContainers;

    private String productId;

}

