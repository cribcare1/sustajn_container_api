package com.sustajn.oderservice.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderListDetails {
    private Long orderId;
    private Long restaurantId;
    private String restaurantName;
    private Integer productCount;
    private Integer totalContainerCount;
    private String orderDate;
    private String orderTime;
    private List<ProductOrderListResponse> productOrderListResponseList;
}
