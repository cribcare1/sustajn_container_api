package com.inventory.request;

import lombok.*;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class AdminOrderApproveRequest {

    private Long orderId;
    private String adminRemark;   // optional approval note

    private List<ItemApproval> items;

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ItemApproval {
        private Long itemId;
        private Integer approvedQty;
    }
}

