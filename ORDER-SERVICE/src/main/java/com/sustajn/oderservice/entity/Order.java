package com.sustajn.oderservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "orders",
        indexes = {
                @Index(name = "idx_orders_user_id", columnList = "userId"),
                @Index(name = "idx_orders_order_date", columnList = "orderDate"),
                @Index(name = "idx_orders_transaction_id", columnList = "transactionId", unique = true)
        }
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Order extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    private LocalDateTime orderDate;

    @Column(nullable = false, unique = true)
    private String transactionId;

    //private String typeOfOrder;

    private String orderStatus;
}
