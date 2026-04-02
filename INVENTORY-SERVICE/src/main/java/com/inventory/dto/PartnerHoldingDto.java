package com.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerHoldingDto {
    private Long restaurantId;
    private String partnerName;
    private String address;
    private Integer count;
}