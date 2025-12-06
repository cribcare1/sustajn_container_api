package com.inventory.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "subscription_plans", indexes = {
    @Index(name = "idx_plan_id", columnList = "plan_id"),
    @Index(name = "idx_plan_status", columnList = "plan_status")
})
@Getter
@Setter
public class SubscriptionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "plan_id")
    private Integer planId;

    @Column(name = "plan_name", length = 150)
    private String planName;

    @Column(name = "plan_type", length = 150)
    private String planType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "partner_type", length = 150)
    private String partnerType;

    @Column(name = "fee_type", precision = 10, scale = 2)
    private BigDecimal feeType;

    @Column(name = "deposit_type", precision = 10, scale = 2)
    private BigDecimal depositType;

    @Column(name = "commission_percentage", precision = 5, scale = 2)
    private BigDecimal commissionPercentage;

    @Column(name = "min_containers")
    private Integer minContainers;

    @Column(name = "max_containers")
    private Integer maxContainers;

    @Column(name = "total_containers")
    private Integer totalContainers;

    @Column(name = "includes_delivery")
    private Boolean includesDelivery;

    @Column(name = "includes_marketing")
    private Boolean includesMarketing;

    @Column(name = "includes_analytics")
    private Boolean includesAnalytics;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", length = 20)
    private BillingCycle billingCycle;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_status", length = 20)
    private PlanStatus planStatus;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public SubscriptionPlan() {
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        computeTotalContainers();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        computeTotalContainers();
    }

    private void computeTotalContainers() {
        if (this.maxContainers != null) {
            this.totalContainers = this.maxContainers;
        } else if (this.minContainers != null) {
            this.totalContainers = this.minContainers;
        } else {
            this.totalContainers = 0;
        }
    }

    public enum BillingCycle {
        MONTHLY,
        QUARTERLY,
        ANNUALLY
    }

    public enum PlanStatus {
        ACTIVE,
        INACTIVE,
        ARCHIVED
    }
}
