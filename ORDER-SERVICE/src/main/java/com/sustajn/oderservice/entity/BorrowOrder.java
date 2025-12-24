package com.sustajn.oderservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "borrow_orders",   indexes = {
        @Index(name = "idx_borrow_orders_order_id", columnList = "orderId"),
        @Index(name = "idx_borrow_orders_user_id", columnList = "userId"),
        @Index(name = "idx_borrow_orders_product_id", columnList = "productId"),
        @Index(name = "idx_borrow_orders_due_date", columnList = "dueDate")
//        @Index(name = "idx_borrow_orders_borrowed_at", columnList = "borrowedAt"),
//        @Index(
//                name = "idx_borrow_orders_user_product",
//                columnList = "userId, productId"
//        )
}
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BorrowOrder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long restaurantId;

    @Column(nullable = false)
    private Long orderId;      // FK reference only

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private int returnedQuantity = 0;

    @Column(nullable = false)
    private LocalDateTime borrowedAt;

    @Column(nullable = false)
    private LocalDateTime dueDate;

    // getters & setters
}
