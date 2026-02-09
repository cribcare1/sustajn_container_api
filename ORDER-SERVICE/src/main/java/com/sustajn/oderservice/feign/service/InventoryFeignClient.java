package com.sustajn.oderservice.feign.service;

import com.sustajn.oderservice.config.FeignClientConfig;
import com.sustajn.oderservice.dto.ApiResponse;
import com.sustajn.oderservice.dto.ProductResponse;
import com.sustajn.oderservice.dto.RestaurantContainerInventoryResponse;
import com.sustajn.oderservice.dto.RestaurantOrderedResponse;
import com.sustajn.oderservice.request.ReduceInventoryRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

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

    @GetMapping("/inventory/getAllResturantInventory/{restaurantId}")
    ApiResponse<List<RestaurantContainerInventoryResponse>> getRestaurantContainerInventoryByRestaurantId(
            @PathVariable("restaurantId") Long restaurantId
    );

    @PostMapping("/inventory/reduceAvailableContainers")
    ApiResponse<?> reduceAvailableContainers(@RequestBody ReduceInventoryRequest request);

    @PostMapping("/inventory/checkAvailabilityOfContainers")
    Map<String, Object> checkAvailability(@RequestBody ReduceInventoryRequest inventoryRequest);

    @PostMapping("/inventory/increaseAvailableContainers")
    Map<String, Object> increaseContainers(@RequestBody ReduceInventoryRequest inventoryRequest);
}
