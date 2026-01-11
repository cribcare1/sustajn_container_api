package com.auth.model;



import com.auth.enumDetails.AccountStatus;
import com.auth.enumDetails.DeviceOS;
import com.auth.enumDetails.Gender;
import com.auth.enumDetails.UserType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_email", columnList = "email"),
        @Index(name = "idx_user_id", columnList = "user_id"),
//        @Index(name = "idx_city_id", columnList = "city_id"),
        @Index(name = "idx_account_status", columnList = "account_status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", length = 50)
    private UserType userType;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(name = "customer_id", length = 100, unique = true)
    private String customerId;


    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 20)
    private Gender gender;

    @Column(name = "profile_picture_url", length = 500)
    private String profilePictureUrl;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "email", length = 250, unique = false)
    private String email;

    @Column(name = "phone_number", length = 25)
    private String phoneNumber;

    @Column(name = "password_hash", length = 250)
    @JsonIgnore
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", length = 50)
    private AccountStatus accountStatus;

    @Column(name = "address", length = 500)
    private String address;

//    @Column(name = "city_id")
//    private Integer cityId;

    @Column(name = "latitude", precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(name = "phone_verified")
    private Boolean phoneVerified;

    @Column(name = "email_verified")
    private Boolean emailVerified;

    @Column(name = "app_version", length = 25)
    private String appVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_os", length = 25)
    private DeviceOS deviceOs;

    @Column(name = "push_notification")
    private Boolean pushNotification;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "subscription_plan_id")
    private Integer subscriptionPlanId;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }


    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

}

