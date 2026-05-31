package com.sustajn.oderservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "fine_ledger")
public class FineLedger extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long borrowOrderId;   // FK as ID only

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private int fineDays;

    @Column(nullable = false)
    private BigDecimal fineAmount;

    @Column(nullable = false)
    private LocalDateTime calculatedAt;

    @Column(nullable = false)
    private boolean isPaid = false;

}

