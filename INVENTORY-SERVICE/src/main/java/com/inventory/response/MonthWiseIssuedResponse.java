package com.inventory.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MonthWiseIssuedResponse {

    private String monthYear; // e.g., "January-2026"
    private Integer totalIssuedQuantity;
    private List<DateWiseIssuedResponse> dateWiseIssuedDetails;
}
