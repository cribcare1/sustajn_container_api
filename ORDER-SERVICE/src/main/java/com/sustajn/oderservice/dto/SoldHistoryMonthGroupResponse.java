package com.sustajn.oderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SoldHistoryMonthGroupResponse {
    private String monthYear;
    private Integer totalQuantity;
    private List<SoldHistoryItemResponse> items;
}