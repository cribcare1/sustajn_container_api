package com.sustajn.oderservice.dto;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyOrderedResponse {

    private String monthYear;
    private int monthTotal;
    private List<DailyOrderedItem> dailyOrders;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyOrderedItem {
        private String date;
        private int quantity;
    }
}