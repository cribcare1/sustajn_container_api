package com.inventory.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminOrderRejectRequest {
    private Long orderId;
    private String adminRemark; // The reason for rejection (e.g., "Insufficient inventory stock")
}