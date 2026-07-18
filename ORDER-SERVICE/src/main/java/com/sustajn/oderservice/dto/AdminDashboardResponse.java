package com.sustajn.oderservice.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardResponse {
    private int containersCirculation;
    private int activeContainers;
    private int overdueContainers;

    private int todayLeased;
    private String leasedTrendPercentage;   // e.g., "+12%"

    private int todayReturns;
    private String returnsTrendPercentage;  // e.g., "-4%"

    private BigDecimal extendedFeeRevenue;
    private BigDecimal soldRevenue;

    private double averageReturnTimeDays;   // e.g., 3.2
    private int activeUsersToday;

    private PopularProductInfo mostLeased;
    private PopularProductInfo lessLeased;
}