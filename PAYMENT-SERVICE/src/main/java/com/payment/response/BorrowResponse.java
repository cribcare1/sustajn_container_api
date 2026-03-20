package com.payment.response;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BorrowResponse {

    private Long id;

    private Long restaurantId;

    private Long orderId;      // FK reference only

    private Long userId;

    private Long productId;

    private int quantity;

    private int returnedQuantity;

    private LocalDateTime borrowedAt;

    private LocalDateTime dueDate;

    private Boolean isExtended;
    private LocalDateTime extendedAt;
    private LocalDateTime effectiveDueDate;
}
