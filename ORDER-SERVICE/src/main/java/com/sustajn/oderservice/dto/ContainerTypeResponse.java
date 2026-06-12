package com.sustajn.oderservice.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ContainerTypeResponse {
    private Integer id;
    private String name;
    private String description;
    private Integer capacityMl;
    private String productId; // This holds codes like "ST-RDC-500"
    private String imageUrl;
    private BigDecimal costPerUnit;
    private String status;
}