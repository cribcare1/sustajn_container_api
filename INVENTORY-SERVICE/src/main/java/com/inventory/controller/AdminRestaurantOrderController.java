package com.inventory.controller;

import com.inventory.response.ApiResponse;
import com.inventory.service.AdminRestaurantOrderService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    // get all issued products to restaurant
    @GetMapping("/issuedProducts/{restaurantId}")
    public ResponseEntity<ApiResponse> getAllIssuedProductsToRestaurant(@PathVariable Long restaurantId) {
        ApiResponse response = adminRestaurantOrderService.getAllIssuedProductsToRestaurant(restaurantId);
        return ResponseEntity.ok(response);
    }

    // get month wise issued products to restaurant
    @GetMapping("/monthWiseIssuedProducts")
    public ResponseEntity<ApiResponse> getMonthWiseIssuedProductsToRestaurant(@RequestParam Long restaurantId, @RequestParam Integer productId) {
        ApiResponse response = adminRestaurantOrderService.getMonthWiseIssuedProductsToRestaurant(restaurantId, productId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/returnedProducts/{restaurantId}")
    public ResponseEntity<ApiResponse> getAllReturnedProductsToRestaurant(@PathVariable Long restaurantId) {
        ApiResponse response = adminRestaurantOrderService.getAllReturnedProductsToRestaurant(restaurantId);
        return ResponseEntity.ok(response);
    }

    // 2. Get Detailed Return History (Month-wise)
    @GetMapping("/monthWiseReturnedProducts")
    public ResponseEntity<ApiResponse> getMonthWiseReturnedProductsToRestaurant(
            @RequestParam Long restaurantId,
            @RequestParam Integer productId) {

        ApiResponse response = adminRestaurantOrderService.getMonthWiseReturnedProductsToRestaurant(restaurantId, productId);
        return ResponseEntity.ok(response);
    }

}
