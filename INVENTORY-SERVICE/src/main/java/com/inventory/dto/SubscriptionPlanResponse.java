package com.inventory.dto;

import java.time.LocalDateTime;

import com.inventory.entity.SubscriptionPlan;

public class SubscriptionPlanResponse {

    private String message;
    private String status;
    private SubscriptionPlan subscriptionPlan;
    private LocalDateTime timestamp;

    public SubscriptionPlanResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public SubscriptionPlanResponse(String message, String status, SubscriptionPlan subscriptionPlan) {
        this.message = message;
        this.status = status;
        this.subscriptionPlan = subscriptionPlan;
        this.timestamp = LocalDateTime.now();
    }

    public static SubscriptionPlanResponse success(String message, SubscriptionPlan plan) {
        return new SubscriptionPlanResponse(message, "success", plan);
    }

    public static SubscriptionPlanResponse failure(String message, String status) {
        return new SubscriptionPlanResponse(message, status, null);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public SubscriptionPlan getSubscriptionPlan() {
        return subscriptionPlan;
    }

    public void setSubscriptionPlan(SubscriptionPlan subscriptionPlan) {
        this.subscriptionPlan = subscriptionPlan;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
