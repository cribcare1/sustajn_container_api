package com.sustajn.oderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContainerChartResponse {
    private Integer total;      // Total capacity (e.g., 1000)
    private Integer lease;      // Total borrowed
    private Integer receive;    // Total returned
    private Integer damage;     // Total damaged
    private Integer available;  // Currently available
    private String monthYear;   // e.g., "November-2025"
}