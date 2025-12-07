package com.inventory.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdminRestaurantDashboardResponse {
    private Integer totalRegisteredRestaurants;
    private Integer totalActiveRestaurants;
    private Integer totalIssuedContainers;
    private Integer totalReturnedContainers;
}
