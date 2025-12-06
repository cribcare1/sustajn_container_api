package com.inventory.dto;

import com.inventory.entity.SubscriptionPlan.BillingCycle;
import com.inventory.entity.SubscriptionPlan.PlanStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Minimal projection/DTO for SubscriptionPlan to return only summary fields.
 */
@Data
@AllArgsConstructor
public class SubscriptionPlanSummary {
    private Integer planId;
    private String planName;
    private PlanStatus planStatus;
    private Integer totalContainers;
    private BillingCycle billingCycle;
}
