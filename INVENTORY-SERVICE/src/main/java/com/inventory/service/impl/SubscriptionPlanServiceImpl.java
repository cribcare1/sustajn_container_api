package com.inventory.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.inventory.Constant.InventoryConstant;
import com.inventory.feignClient.AuthFeignClient;
import com.inventory.request.SubscriptionRequest;
import com.inventory.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.inventory.entity.SubscriptionPlan;
import com.inventory.repository.SubscriptionPlanRepository;
import com.inventory.service.SubscriptionPlanService;
import com.inventory.dto.SubscriptionPlanSummary;
import org.springframework.util.CollectionUtils;

@Service
@RequiredArgsConstructor
public class SubscriptionPlanServiceImpl implements SubscriptionPlanService {

    private final SubscriptionPlanRepository repository;
    private final AuthFeignClient authFeignClient;

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

    //need to change based on roles
    @Override
    public ApiResponse<List<SubscriptionPlan>> listAllPlansBasedOnRoles(String role) {
        try {
            // Using repository.findAll() which we annotated with @EntityGraph to avoid n+1
            List<SubscriptionPlan> planList = repository.findAllPlans(role);
            if (CollectionUtils.isEmpty(planList)){
                return new ApiResponse<>("error", "No subscription plans found", null);
            }
            return new ApiResponse<>("success", "Subscription plans retrieved", planList);
        } catch (Exception ex) {
            return new ApiResponse<>("error", "Failed to retrieve subscription plans: " + ex.getMessage(), null);
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


    @Override
    public Map<String, Object> upgradeSubscriptionDetails(SubscriptionRequest subscriptionRequest) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Validate user ID
            if (subscriptionRequest.getUserId() == null) {
                response.put(InventoryConstant.STATUS, InventoryConstant.ERROR);
                response.put(InventoryConstant.MESSAGE, "User ID is required!");
                return response;
            }
            // Validate subscription plan ID
            if (subscriptionRequest.getSubscriptionPlanId() == null) {
                response.put(InventoryConstant.STATUS, InventoryConstant.ERROR);
                response.put(InventoryConstant.MESSAGE, "Subscription Plan ID is required!");
                return response;
            }
            // Fetch the subscription plan
            Optional<SubscriptionPlan> subscriptionPlanOptional = repository.findById(subscriptionRequest.getSubscriptionPlanId());
            if (subscriptionPlanOptional.isEmpty()) {
                response.put(InventoryConstant.STATUS, InventoryConstant.ERROR);
                response.put(InventoryConstant.MESSAGE, "Subscription Plan not found!");
                return response;
            }

            // Validate User's current subscription and upgrade logic here
            Map<String,Object> userResponse = authFeignClient.getUserDetails(subscriptionRequest.getUserId());
            if (!InventoryConstant.SUCCESS.equals(userResponse.get(InventoryConstant.STATUS))) {
                response.put(InventoryConstant.STATUS, InventoryConstant.ERROR);
                response.put(InventoryConstant.MESSAGE, "User not found!");
                return response;
            }

            // Update user's subscription details via Auth Service
            Map<String, Object> updateResponse = authFeignClient.upgradeSubscription(subscriptionRequest);
            if (!InventoryConstant.SUCCESS.equals(updateResponse.get(InventoryConstant.STATUS))) {
                response.put(InventoryConstant.STATUS, InventoryConstant.ERROR);
                response.put(InventoryConstant.MESSAGE, "Failed to upgrade subscription!");
                return response;
            }

            response.put(InventoryConstant.STATUS, InventoryConstant.SUCCESS);
            response.put(InventoryConstant.MESSAGE, "Subscription upgraded successfully!");
            return response;

        } catch (Exception e) {
            response.put(InventoryConstant.STATUS, InventoryConstant.ERROR);
            response.put(InventoryConstant.MESSAGE, "Failed to update subscription details!");
            response.put(InventoryConstant.DETAILS, e.getMessage());
        }

        return response;
    }


}
