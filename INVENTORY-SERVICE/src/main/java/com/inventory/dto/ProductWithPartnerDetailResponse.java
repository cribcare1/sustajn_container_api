package com.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductWithPartnerDetailResponse {
    private Integer containerTypeId;
    private String name;
    private String productId;
    private String capacity;
    private String imageUrl;
    private Integer totalWithPartner;

    // The list of restaurants holding this container
    private List<PartnerHoldingDto> partners;
}