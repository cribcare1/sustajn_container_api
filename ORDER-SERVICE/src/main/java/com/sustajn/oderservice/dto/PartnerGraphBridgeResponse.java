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
public class PartnerGraphBridgeResponse {
    private String monthYear; // e.g., "November-2025"
    private List<DailyStat> dailyStats;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DailyStat {
        private int day;          // e.g., 1, 2, 3
        private String dayName;   // e.g., "Mon", "Tue"
        private int leased;       // Yellow bar value
        private int returned;     // White bar value
    }
}