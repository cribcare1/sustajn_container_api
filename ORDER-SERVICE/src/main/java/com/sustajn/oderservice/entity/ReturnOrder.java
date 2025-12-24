package com.sustajn.oderservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "return_orders",
        indexes = {
                @Index(name = "idx_return_orders_borrow_order_id", columnList = "borrowOrderId"),
                @Index(name = "idx_return_orders_user_id", columnList = "userId"),
                @Index(name = "idx_return_orders_product_id", columnList = "productId"),
                @Index(name = "idx_return_orders_returned_at", columnList = "returnedAt"),
                @Index(
                        name = "idx_return_orders_user_product",
                        columnList = "userId, productId"
                )
        }
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReturnOrder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK reference as ID only (microservice-safe)
    @Column(nullable = false)
    private Long borrowOrderId;

    private Long restaurantId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private int returnedQuantity;

    @Column(nullable = false)
    private LocalDateTime returnedAt;

}
