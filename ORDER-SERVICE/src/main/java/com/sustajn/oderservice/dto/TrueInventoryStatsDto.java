package com.sustajn.oderservice.dto;

import lombok.Data;

@Data
public class TrueInventoryStatsDto {
    private Integer total;
    private Integer available;
    private Integer damageCount;
}