package com.inventory.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DateWiseReturnedResponse {
    private String date; // e.g., "25.11.2025"
    private Integer todayTotalReturnedQuantity;
}