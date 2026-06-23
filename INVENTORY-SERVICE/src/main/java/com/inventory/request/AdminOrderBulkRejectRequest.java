package com.inventory.request;

import lombok.*;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class AdminOrderBulkRejectRequest {
    private List<Long> orderIds; // Accepts an array of IDs [12, 13]
    private String adminRemark;
}