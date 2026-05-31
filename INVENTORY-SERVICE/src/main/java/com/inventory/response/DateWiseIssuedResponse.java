package com.inventory.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DateWiseIssuedResponse {

    private String date; // e.g., "01.01.2026"
    private Integer todayTotalIssuedQuantity;
}
