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
public class SoldContainersDateWiseResponse {

    private  String productIds; // productId1 | productId2 | productId3
    private String LocalDateTime; // 20.02.2026 | 10:15
    private Integer dateWiseTotalDamageContainers;
    private List<SoldProductResponse> products;
}
