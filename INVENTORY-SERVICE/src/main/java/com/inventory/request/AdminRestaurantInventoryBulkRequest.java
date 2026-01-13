package com.inventory.request;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminRestaurantInventoryBulkRequest {

    private Long restaurantId;   // required
    private Long createdBy;      // logged admin

    private List<ContainerEntry> containers;  // multiple container entries

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ContainerEntry {
        private Integer containerTypeId;
        private Integer containerCount;
        private String actionType;  // BORROW / RETURN
    }
}

