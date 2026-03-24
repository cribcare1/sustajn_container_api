package com.inventory.feignClient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "ORDER-SERVICE")
public interface OrderFeignClient {

    @GetMapping("/orders/internal/circulation-count/{productId}")
    Integer getInCirculationCount(@PathVariable("productId") Long productId);
}