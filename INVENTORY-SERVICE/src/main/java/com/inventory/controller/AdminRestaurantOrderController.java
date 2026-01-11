package com.inventory.controller;

import com.inventory.response.ApiResponse;
import com.inventory.service.AdminRestaurantOrderService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/inventory/restaurantOrders")
@RequiredArgsConstructor
public class AdminRestaurantOrderController {

    private final AdminRestaurantOrderService adminRestaurantOrderService;

    @GetMapping("/orderHistory/{restaurantId}")
    public ResponseEntity<ApiResponse> getOrderHistory(@PathVariable Long restaurantId){
        ApiResponse response = adminRestaurantOrderService.getRestaurantOrderDetails(restaurantId);
        return ResponseEntity.ok(response);
    }

}
