package com.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerSoldHistoryRawDto {
    private Long productId;
    private Integer soldQuantity;
    private Long unitPrice;
    private Long totalAmount;
    private LocalDateTime borrowedAt;
    private LocalDateTime dueDate;
    private LocalDateTime soldAt;
}