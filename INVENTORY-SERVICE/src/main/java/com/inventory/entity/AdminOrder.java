package com.inventory.entity;

import com.inventory.Constant.AdminOrderStatus;
import com.inventory.Constant.TransactionType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "admin_orders",
        indexes = {
                @Index(name = "idx_admin_orders_restaurant", columnList = "restaurantId"),
                @Index(name = "idx_admin_orders_status", columnList = "status")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class AdminOrder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long restaurantId;

    private String orderId;        // reference to order service order ID

    private LocalDateTime orderDate;

    @Enumerated(EnumType.STRING)
    private AdminOrderStatus status;   // PENDING / APPROVED / REJECTED

    @Enumerated(EnumType.STRING)
    private TransactionType type;     // BORROW / RETURN

    private String restaurantRemark;    // message from restaurant

    private String adminRemark;        // rejection/approval message

    private LocalDateTime decisionAt;  // when admin acted

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
    private List<AdminOrderItem> items;

    @PrePersist
    public void generateOrderId() {
        if (orderId == null && id != null) {
            this.orderId = "ORD-" + id;
        }
    }
}

