package com.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnedDetailResponse {
    private Long orderId;
    private Long restaurantId;
    private String restaurantName;
    private String restaurantAddress;
    private String containerCode;
    private Integer quantity;
    private String returnedOn;
    private String collectedOn;
}