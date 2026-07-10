package com.sustajn.oderservice.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtendedFeeMonthWiseResponse {
    private String monthYear;                 // e.g., "November-2025"
    private BigDecimal monthTotalAmount;      // Cumulative monthly fee sum
    private List<ExtendedFeeTransactionResponse> transactions;
}