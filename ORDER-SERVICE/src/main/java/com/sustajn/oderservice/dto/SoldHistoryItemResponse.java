package com.sustajn.oderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SoldHistoryItemResponse {
    private Long id;
    private Long userId;
    private String customerId;
    private Long restaurantId;
    private String restaurantName;
    private Long productId;
    private String containerCode;
    private Integer soldQuantity;
    private String soldDate;
    private String soldTime;
    private String fullDateTime;
    private String reason;
}