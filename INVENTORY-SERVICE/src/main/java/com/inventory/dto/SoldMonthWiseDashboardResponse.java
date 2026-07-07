package com.inventory.dto;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SoldMonthWiseDashboardResponse {
    private String monthYear;                 // Section header title "November-2025"
    private Integer monthTotalAmount;          // Combined monthly revenue aggregate (e.g., 3000)
    private List<SoldTransactionResponse> transactions;
}