package com.inventory.request;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InventoryBulkAddRequest {

    private Long createdBy;

    private List<InventorySingleAddRequest> containers;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventorySingleAddRequest {
        private Integer containerTypeId;
        private Integer quantity;   // How many containers to add
    }
}

