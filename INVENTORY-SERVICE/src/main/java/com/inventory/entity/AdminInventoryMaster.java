package com.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import com.inventory.util.DateTimeUtil;

@Entity
@Table(
    name = "admin_inventory_master",
    indexes = {
        @Index(name = "idx_container_type_id", columnList = "container_type_id"),
        @Index(name = "idx_inventory_status", columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminInventoryMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "container_type_id", nullable = false)
    private Integer containerTypeId;   // FK only using id

    @Column(name = "total_containers", nullable = false)
    private Integer totalContainers = 0;

    @Column(name = "available_containers", nullable = false)
    private Integer availableContainers = 0;

    @Column(name = "status", length = 15)
    private String status = "active"; // active/inactive/discontinued

    // Audit Fields
    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime nowDubai = DateTimeUtil.nowDubai();
        this.createdAt = nowDubai;
        this.updatedAt = nowDubai;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = DateTimeUtil.nowDubai();
    }
}
