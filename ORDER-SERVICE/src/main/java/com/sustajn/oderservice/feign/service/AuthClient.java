package com.sustajn.oderservice.feign.service;

import com.sustajn.oderservice.config.FeignClientConfig;
import com.sustajn.oderservice.dto.RestaurantRegisterResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "AUTH-SERVICE",configuration = FeignClientConfig.class)
public interface AuthClient {
    @PostMapping(
            value = "/auth/getRestaurants",
            consumes = "application/json"
    )
    List<RestaurantRegisterResponse> getRestaurantsByIds(
            @RequestBody List<Long> ids
    );

    @GetMapping(value = "/auth/getUserIdByCustomerId")
    Long getUserIdByCustomerId(@RequestParam String customerId);
}
