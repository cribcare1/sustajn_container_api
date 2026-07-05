package com.inventory.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContainerExtensionInfo {
    private Integer id;
    private String name;
    private String productId;
    private Integer capacityMl;
    private String imageUrl;
    private BigDecimal extendFee;
}