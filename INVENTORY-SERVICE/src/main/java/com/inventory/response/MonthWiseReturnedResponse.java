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
public class MonthWiseReturnedResponse {
    private String monthYear; // e.g., "November-2025"
    private Integer totalReturnedQuantity; // Total for that month
    private List<DateWiseReturnedResponse> dateWiseReturnedDetails;
}