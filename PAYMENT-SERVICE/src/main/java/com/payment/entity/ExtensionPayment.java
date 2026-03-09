package com.payment.entity;

import com.payment.constants.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "extension_payment")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ExtensionPayment {

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

    private LocalDateTime paidAt;

}
