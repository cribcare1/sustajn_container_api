package com.sustajn.oderservice.entity;

import com.sustajn.oderservice.util.DateTimeUtil;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sold_orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SoldOrder extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;
    private Long userId;
    private Long productId;
    private Long restaurantId;

    private Integer soldQuantity;

    private Long unitPrice;
    private Long totalAmount;

    private Long paymentId;
    private String stripePaymentIntentId;

    private String reason; // AUTO_SOLD / MANUAL

    private LocalDateTime soldAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = DateTimeUtil.nowDubai();
        this.updatedAt = DateTimeUtil.nowDubai();
        this.soldAt = DateTimeUtil.nowDubai();
    }
}
