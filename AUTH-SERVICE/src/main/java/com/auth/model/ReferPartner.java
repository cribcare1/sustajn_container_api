package com.auth.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "refer_partners")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReferPartner{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "business_name", length = 200)
    private String businessName;
    @Column(name = "contact_person_name", length = 100)
    private String contactPersonName;
    @Column(name = "contact_email", length = 150)
    private String contactEmail;
    @Column(name = "contact_phone", length = 50)
    private String contactPhone;
    @Column(name = "referred_by_user_id")
    private Long referredByUserId;
    @Column(name = "status", length = 150)
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
