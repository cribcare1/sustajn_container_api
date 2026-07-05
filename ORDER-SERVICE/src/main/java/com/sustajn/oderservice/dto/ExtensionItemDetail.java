package com.sustajn.oderservice.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtensionItemDetail {
    private Long containerTypeId;
    private String containerName;   // e.g., "Dip Cup"
    private String productCode;     // e.g., "ST-DC-50"
    private String capacity;        // e.g., "50ml"
    private String imageUrl;
    private Integer quantityToExtend; // (quantity - returnedQuantity)
}