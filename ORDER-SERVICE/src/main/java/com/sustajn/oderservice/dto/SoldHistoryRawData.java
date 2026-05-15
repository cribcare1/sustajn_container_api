package com.sustajn.oderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SoldHistoryRawData {
    private Long productId;
    private Integer soldQuantity;
    private Long unitPrice;
    private LocalDateTime borrowedAt;
    private LocalDateTime dueDate;
    private LocalDateTime soldAt;
}