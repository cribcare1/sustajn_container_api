package com.sustajn.oderservice.feign.service;

import com.inventory.response.RestaurantOrderedResponse;
import com.sustajn.oderservice.config.FeignClientConfig;
import com.sustajn.oderservice.dto.ApiResponse;
import com.sustajn.oderservice.dto.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @GetMapping("/inventory/restaurantOrders/orderHistory/{restaurantId}")
    ApiResponse<List<RestaurantOrderedResponse>> getOrderHistory(
            @PathVariable("restaurantId") Long restaurantId
    );

}
