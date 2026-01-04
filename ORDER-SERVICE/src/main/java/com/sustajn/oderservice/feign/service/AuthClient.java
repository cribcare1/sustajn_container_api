package com.sustajn.oderservice.feign.service;

import com.sustajn.oderservice.config.FeignClientConfig;
import com.sustajn.oderservice.dto.RestaurantRegisterResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

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
}
