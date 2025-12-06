package com.inventory.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.inventory.entity.SubscriptionPlan;
import com.inventory.repository.SubscriptionPlanRepository;
import com.inventory.service.SubscriptionPlanService;
import com.inventory.dto.SubscriptionPlanSummary;

@Service
public class SubscriptionPlanServiceImpl implements SubscriptionPlanService {

    private final SubscriptionPlanRepository repository;

    @Autowired
    public SubscriptionPlanServiceImpl(SubscriptionPlanRepository repository) {
        this.repository = repository;
    }

    private Map<String, Object> buildResponse(String message, String status, Object data) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("message", message);
        resp.put("status", status);
        resp.put("data", data);
        return resp;
    }

    @Override
    public Map<String, Object> createSubscriptionPlan(SubscriptionPlan plan) {
        try {
            SubscriptionPlan saved = repository.save(plan);
            return buildResponse("Subscription plan created", "success", saved);
        } catch (Exception ex) {
            return buildResponse("Failed to create subscription plan: " + ex.getMessage(), "error", null);
        }
    }

    @Override
    public Map<String, Object> getSubscriptionPlanById(Integer id) {
        try {
            Optional<SubscriptionPlan> opt = repository.findById(id);
            if (opt.isPresent()) {
                return buildResponse("Subscription plan found", "success", opt.get());
            } else {
                return buildResponse("Subscription plan not found", "not_found", null);
            }
        } catch (Exception ex) {
            return buildResponse("Error fetching subscription plan: " + ex.getMessage(), "error", null);
        }
    }

    @Override
    public Map<String, Object> updateSubscriptionPlan(Integer id, SubscriptionPlan plan) {
        try {
            Optional<SubscriptionPlan> opt = repository.findById(id);
            if (opt.isEmpty()) {
                return buildResponse("Subscription plan not found", "not_found", null);
            }

            SubscriptionPlan existing = opt.get();

            // Update fields (only simple copy - adjust to your rules)
            existing.setPlanName(plan.getPlanName());
            existing.setPlanType(plan.getPlanType());
            existing.setDescription(plan.getDescription());
            existing.setPartnerType(plan.getPartnerType());
            existing.setFeeType(plan.getFeeType());
            existing.setDepositType(plan.getDepositType());
            existing.setCommissionPercentage(plan.getCommissionPercentage());
            existing.setMinContainers(plan.getMinContainers());
            existing.setMaxContainers(plan.getMaxContainers());
            existing.setIncludesDelivery(plan.getIncludesDelivery());
            existing.setIncludesMarketing(plan.getIncludesMarketing());
            existing.setIncludesAnalytics(plan.getIncludesAnalytics());
            existing.setBillingCycle(plan.getBillingCycle());
            existing.setPlanStatus(plan.getPlanStatus());

            SubscriptionPlan saved = repository.save(existing);
            return buildResponse("Subscription plan updated", "success", saved);
        } catch (Exception ex) {
            return buildResponse("Failed to update subscription plan: " + ex.getMessage(), "error", null);
        }
    }

    @Override
    public Map<String, Object> deleteSubscriptionPlan(Integer id) {
        try {
            if (!repository.existsById(id)) {
                return buildResponse("Subscription plan not found", "not_found", null);
            }
            repository.deleteById(id);
            return buildResponse("Subscription plan deleted", "success", id);
        } catch (Exception ex) {
            return buildResponse("Failed to delete subscription plan: " + ex.getMessage(), "error", null);
        }
    }

    @Override
    public Map<String, Object> listAllPlans() {
        try {
            // Using repository.findAll() which we annotated with @EntityGraph to avoid n+1
            List<SubscriptionPlan> list = repository.findAll();
            return buildResponse("Subscription plans retrieved", "success", list);
        } catch (Exception ex) {
            return buildResponse("Failed to list subscription plans: " + ex.getMessage(), "error", null);
        }
    }

    @Override
    public Map<String, Object> getPlanSummaries(SubscriptionPlan.PlanStatus status) {
        try {
            List<SubscriptionPlanSummary> summaries = repository.findSummariesByStatus(status);
            return buildResponse("Subscription plan summaries retrieved", "success", summaries);
        } catch (Exception ex) {
            return buildResponse("Failed to retrieve summaries: " + ex.getMessage(), "error", null);
        }
    }

}
