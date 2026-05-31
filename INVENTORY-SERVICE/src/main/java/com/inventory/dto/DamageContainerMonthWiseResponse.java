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
public class DamageContainerMonthWiseResponse {

    private String monthYear; // e.g., "January-2026"
    private Integer monthWiseTotalDamageContainers;
    private List<DamageContainerDateWiseResponse> damageContainers;
}
