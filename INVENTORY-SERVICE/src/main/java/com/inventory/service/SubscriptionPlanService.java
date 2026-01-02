package com.inventory.service;

import java.util.Map;

import com.inventory.entity.SubscriptionPlan;
import com.inventory.request.SubscriptionRequest;

public interface SubscriptionPlanService {

    Map<String, Object> createSubscriptionPlan(SubscriptionPlan plan);

    Map<String, Object> getSubscriptionPlanById(Integer id);

    Map<String, Object> updateSubscriptionPlan(Integer id, SubscriptionPlan plan);

    Map<String, Object> deleteSubscriptionPlan(Integer id);

    Map<String, Object> listAllPlans();

    /**
     * Return minimal summaries for subscription plans. If status is null, returns all summaries.
     */
    Map<String, Object> getPlanSummaries(SubscriptionPlan.PlanStatus status);

    Map<String, Object> upgradeSubscriptionDetails(SubscriptionRequest subscriptionRequest);

}
