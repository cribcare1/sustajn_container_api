package com.sustajn.oderservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "product_fine_config",
        uniqueConstraints = @UniqueConstraint(columnNames = {"productId"})
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductFineConfig extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long productId;

    private Long subscriptionId;

//    @Column(nullable = false)
//    private String location;

    @Column(nullable = false)
    private BigDecimal finePerDay;

    // getters & setters
}

