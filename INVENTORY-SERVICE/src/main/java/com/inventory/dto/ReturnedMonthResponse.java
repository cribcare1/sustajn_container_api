package com.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnedMonthResponse {
    private String monthYear;
    private Integer totalQuantity;
    private List<ReturnedDetailResponse> returns;
}