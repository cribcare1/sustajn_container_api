package com.auth.feignClient;
import com.auth.response.SubscriptionPlanResponse;

import com.auth.config.FeignMultipartConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "INVENTORY-SERVICE", configuration = FeignMultipartConfig.class)
public interface InventoryFeignClient {

    @GetMapping("/inventory/subscription-plans/getSubscriptionPlan/{id}")
    Map<String, Object> getSubscriptionPlanById(@PathVariable Integer id);


}
