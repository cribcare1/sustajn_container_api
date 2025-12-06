package com.inventory.dto;


import java.math.BigDecimal;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContainerTypeResponse {
    private Integer id;
    private String name;
    private String description;
    private Integer capacityMl;
    private String material;
    private String colour;
    private BigDecimal lengthCm;
    private BigDecimal widthCm;
    private BigDecimal heightCm;
    private Integer weightGrams;
    private Integer maxTemperature;
    private Integer minTemperature;
    private Integer lifespanCycle;
    private String imageUrl;
    private BigDecimal costPerUnit;

}