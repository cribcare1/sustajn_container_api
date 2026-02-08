package com.sustajn.oderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MostUsedContainerResponse {

    private Integer productId;
    private Double percentage;
    private String containerName;
    private String productUniqueId;
    private Integer capacityMl;
    private String imageUrl;

}
