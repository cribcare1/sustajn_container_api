package com.inventory.request;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminOrderCreateRequest {

    private Long restaurantId;
    private String orderId;          // optional reference from order service
    private String restaurantRemark; // reason / note
    private String type;             // BORROW / RETURN

    private List<ItemRequest> items;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ItemRequest {
        private Integer containerTypeId;
        private Integer requestedQty;
    }
}

