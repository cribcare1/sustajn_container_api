package com.sustajn.oderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContainerTypeResponse {
    private Integer id;
    private String name;
    private String description;
    private Integer capacityMl;
    private String productId; // Maps to your container product code string (e.g. ST-RDC-500)
    private String imageUrl;
    private BigDecimal costPerUnit;
    private String status;
}