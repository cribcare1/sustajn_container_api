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

    @Column(name = "bank_name", length = 1000)
    private String bankName;
    @Column(name = "account_holder_name", length = 1000)
    private String accountHolderName;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;


    private String iBanNumber;

    @Column(name = "account_number", length = 200)
    private String accountNumber;

    @Column(name = "bic_number", length = 200)
    private String bicNumber;

    @Column(name = "card_holder_name", length = 100)
    private String cardHolderName;

    @Column(name = "card_number", length = 100)
    private String cardNumber;

    @Column(name = "expiry_date", length = 100)
    private String expiryDate;

    @Column(name = "payment_gateway_id", length = 100)
    private String paymentGatewayId;

    @Column(name = "payment_gateway_name", length = 100)
    private String paymentGatewayName;

    @Column(name = "status", length = 100)
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
