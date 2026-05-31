package com.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DamageProductResponse {

    private String customerId;
    private String restaurantName;
    private Integer productId;
    private String productName;
    private String productDescription;
    private String productImageUrl;
    private Integer capacity;
    private String productUniqueId;
    private String damageRemark;
    private String damageImagesUrls; // #-separated URLs of damage images
}
