package com.inventory.feignClient;

import com.inventory.dto.SoldHistoryRawData;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;

@FeignClient(name = "ORDER-SERVICE")
public interface OrderFeignClient {

    @GetMapping("/orders/internal/circulation-count/{productId}")
    Integer getInCirculationCount(@PathVariable("productId") Long productId);


    @GetMapping("/orders/internal/bulk-circulation-counts")
    Map<Long, Integer> getBulkCirculationCounts();

    @GetMapping("/orders/internal/circulation-by-user/{productId}")
    Map<Long, Integer> getCirculationByUser(@PathVariable("productId") Long productId);

    @GetMapping("/orders/internal/sold-history-dates/{restaurantId}")
    List<SoldHistoryRawData> getRealSoldHistoryDates(@PathVariable("restaurantId") Long restaurantId);

    @GetMapping("/orders/internal/restaurant/user-balances/{restaurantId}")
    Map<Long, Map<String, Integer>> getRestaurantUserBalances(@PathVariable("restaurantId") Long restaurantId);
}