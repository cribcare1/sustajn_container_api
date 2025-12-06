package com.inventory.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RestaurantInventoryViewResponse {

    private Integer containerTypeId;
    private String containerTypeName;
    private Integer totalContainers;
    private Integer availableContainers;
    private Integer borrowedByRestaurant;
    private Integer returnedContainersCount;
    private String status;  // BORROW or RETURN (latest action)
}
