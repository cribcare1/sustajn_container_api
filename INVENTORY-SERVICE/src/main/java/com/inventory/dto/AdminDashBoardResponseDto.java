package com.inventory.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdminDashBoardResponseDto {
private AdminRestaurantDashboardResponse restaurantDashboardResponse;
private AdminCustomerDashboardResponse CustomerDashboardResponse;
}