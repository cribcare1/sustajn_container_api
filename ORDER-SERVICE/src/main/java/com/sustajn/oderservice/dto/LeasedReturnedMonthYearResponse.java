package com.sustajn.oderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LeasedReturnedMonthYearResponse {

    private String monthYear; // Format: "November-2025"
    private Integer totalLeasedOrReturnCount;
    private List<DateWiseReturnCountResponse> dateLeasedReturnCounts;
}
