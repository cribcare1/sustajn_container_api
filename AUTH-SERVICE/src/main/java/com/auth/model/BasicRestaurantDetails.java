package com.auth.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "basic_restaurant_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BasicRestaurantDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long restaurantId;

    @Column
    private String speciality;

    @Column(name = "business_type")
    private String businessType;

    @Column(name = "website_details")
    private String websiteDetails;

    @Column
    private String cuisine;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Auto timestamp handlers
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
