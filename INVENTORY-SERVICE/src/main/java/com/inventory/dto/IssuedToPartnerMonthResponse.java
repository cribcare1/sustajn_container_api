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
public class IssuedToPartnerMonthResponse {
    private String monthYear;          // e.g., "November-2025"
    private Integer totalQuantity;     // e.g., 500
    private List<PartnerIssuedDetailResponse> issuances;
}