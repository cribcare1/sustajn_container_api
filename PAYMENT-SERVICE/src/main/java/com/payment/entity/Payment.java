package com.payment.entity;

import com.payment.constants.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment",
        uniqueConstraints = {
        @UniqueConstraint(columnNames = "stripeSessionId")
        }
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private Long userId;

    private String stripeSessionId;

    private String stripePaymentIntentId;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status; // PENDING, SUCCESS, FAILED

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime paidAt;

    private Long amount;

    private String paymentReason; // extend ot sold

    private String failureReason;

    private String stripeCustomerId;

    private String stripePaymentMethodId;


    private Integer retryCount = 0;

    private LocalDateTime nextRetryAt;

}
