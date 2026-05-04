package com.auth.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "address", indexes = {
    @Index(name = "idx_postal_code", columnList = "po_box_or_postal_code"),
    @Index(name = "idx_address_id", columnList = "id")
})
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "address_type", length = 500)
    private String addressType;

    @Column(name = "flat_door_house_details", length = 1000)
    private String flatDoorHouseDetails;

    @Column(name = "area_street_city_block_details", length = 1500)
    private String areaStreetCityBlockDetails;

    @Column(name = "po_box_or_postal_code", length = 200)
    private String poBoxOrPostalCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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
