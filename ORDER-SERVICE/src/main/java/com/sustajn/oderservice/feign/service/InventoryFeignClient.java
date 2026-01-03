package com.sustajn.oderservice.feign.service;

import com.sustajn.oderservice.config.FeignClientConfig;
import com.sustajn.oderservice.dto.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(
        name = "INVENTORY-SERVICE",
        configuration = FeignClientConfig.class
)
public interface InventoryFeignClient {
    @PostMapping(value = "/inventory/getProductsByIds",consumes = "application/json"
    )
    List<ProductResponse> getProductsByIds(@RequestBody List<Integer> ids);
}
