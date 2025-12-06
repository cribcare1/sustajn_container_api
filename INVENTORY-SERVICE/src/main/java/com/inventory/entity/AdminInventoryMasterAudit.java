package com.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import com.inventory.util.DateTimeUtil;

@Entity
@Table(
    name = "admin_inventory_master_audit",
    indexes = {
        @Index(name = "idx_inventory_master_id", columnList = "inventory_master_id"),
        @Index(name = "idx_action_type", columnList = "action_type")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminInventoryMasterAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Reference to AdminInventoryMaster table
    @Column(name = "inventory_master_id", nullable = false)
    private Long inventoryMasterId;

    // +ve = added, -ve = removed
    @Column(name = "quantity_change", nullable = false)
    private Integer quantityChange;

    // After action, updated available count
    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter;

    @Column(name = "action_type", length = 20, nullable = false)
    private String actionType; // ADD or REMOVE

    @Column(name = "reason", length = 255)
    private String reason; // Ex: damaged, new stock, issued, lost etc.

    @Column(name = "changed_by")
    private Long changedBy;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @PrePersist
    public void onCreate() {
        this.changedAt = DateTimeUtil.nowDubai();
    }
}

