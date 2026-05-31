package com.inventory.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdminCustomerDashboardResponse {
    private Integer totalCustomers;
    private Integer activeCustomers;
    private Integer totalBorrowedItems;
    private Integer overdueItems;
    private Integer totalReturnedItems;
}
