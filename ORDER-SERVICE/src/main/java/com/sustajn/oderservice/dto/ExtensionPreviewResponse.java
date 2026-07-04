package com.sustajn.oderservice.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtensionPreviewResponse {
    private String currentDueDate;      // Displays "03.01.2026"
    private String newDueDate;          // Displays "08.01.2026"
    private BigDecimal totalExtensionFee; // Multiplied item fees used for "Confirm & Pay AED X"

    // 🟢 References the clean standalone class directly
    private List<ExtensionItemDetail> products;
}