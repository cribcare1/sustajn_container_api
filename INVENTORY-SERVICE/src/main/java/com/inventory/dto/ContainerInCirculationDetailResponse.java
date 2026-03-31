package com.inventory.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ContainerInCirculationDetailResponse {
    private Integer containerTypeId;
    private String name;
    private String productId;
    private String capacity;
    private String imageUrl;
    private Integer totalInCirculation;
    private List<UserHoldingDto> users;
}