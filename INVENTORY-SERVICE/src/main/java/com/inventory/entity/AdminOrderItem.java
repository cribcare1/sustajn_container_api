package com.inventory.entity;

import com.inventory.Constant.TransactionType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "admin_order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminOrderItem extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer containerTypeId;     // optional if multiple container types

    private Integer requestedQty;     // restaurant requested quantity
    private Integer approvedQty;      // admin approved qty


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_order_id")
    private AdminOrder order;
}

