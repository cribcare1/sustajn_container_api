package com.inventory.feignClient;

import com.inventory.config.FeignMultipartConfig;
import com.inventory.dto.PartnerInfoDto;
import com.inventory.request.SubscriptionRequest;
import com.inventory.response.ApiResponse;
import com.sustajn.oderservice.dto.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(
        name = "AUTH-SERVICE",
        configuration = FeignMultipartConfig.class
)
public interface AuthFeignClient {

    @GetMapping("/auth/userDetails/{userId}")
    Map<String, Object> getUserDetails(@PathVariable Long userId);

    @PostMapping(value = "/auth/upgradeSubscription")
    Map<String, Object> upgradeSubscription(@RequestBody SubscriptionRequest request);

    @PostMapping("/auth/internal/customer-ids")
    Map<Long, String> getCustomerIdsBulk(@RequestBody List<Long> userIds);

    @PostMapping("/auth/internal/partner-details")
    Map<Long, PartnerInfoDto> getPartnerDetailsBulk(@RequestBody List<Long> userIds);

    @GetMapping(value = "/auth/getUserByCustomerId")
    ApiResponse<UserResponse> getUserByCustomerId(@RequestParam String customerId);
}
