package com.inventory.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserHoldingDto {
    private String userId; // Will hold "ROBE-2323"
    private Integer count;
}