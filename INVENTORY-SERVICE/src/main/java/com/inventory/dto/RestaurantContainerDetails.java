package com.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RestaurantContainerDetails {
    private Integer containerId;
    private String containerName;
    private String containerDescription;
    private Integer capacity;
    private String containerImageUrl;
    private String containerUniqueId;
    private Integer quantityAvailable;
}
