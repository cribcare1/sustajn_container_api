package com.sustajn.oderservice.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserExtendedFeeMonthWiseResponse {
    private String monthYear;                 // e.g., "November-2025"
    private BigDecimal monthTotalAmount;      // Cumulative sum for this specific user
    private List<UserExtendedFeeItemResponse> extensions;
}