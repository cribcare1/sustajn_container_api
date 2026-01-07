package com.auth.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "bank_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Column(name = "bank_name", length = 255)
    private String bankName;
    private String accountNumber;

    private String iBanNumber;

    @Column(name = "tax_number", length = 100)
    private String taxNumber;

    @Column(name = "card_holder_name", length = 100)
    private String cardHolderName;

    @Column(name = "card_number", length = 100)
    private String cardNumber;

    @Column(name = "expiry_date", length = 100)
    private String expiryDate;

    @Column(name = "cvv", length = 100)
    private String cvv;

    @Column(name = "payment_gateway_id", length = 100)
    private String paymentGatewayId;

    @Column(name = "payment_gateway_name", length = 100)
    private String paymentGatewayName;

    @Column(name = "status", length = 50)
    private String status;



    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
