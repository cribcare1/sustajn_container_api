package com.inventory.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionMonthWiseResponse {
    private String monthYear; // Displays "June 2026"
    private BigDecimal monthWiseTotalSusbcriptionAmount; // Matches UI typo spec exactly
    private List<SubscriptionItemDetailResponse> dateWiseSubscription;
}