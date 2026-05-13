package com.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DetailedSoldDateWiseResponse {
    private String productIds;
    private String localDateTime; // e.g., "20.02.2026 | 10:15"
    private Integer dateWiseTotalDamageContainers;
    private List<DetailedSoldProductResponse> products;
}