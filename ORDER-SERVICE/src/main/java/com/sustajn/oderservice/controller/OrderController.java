package com.sustajn.oderservice.controller;

import com.sustajn.oderservice.dto.ApiResponse;
import com.sustajn.oderservice.request.BorrowRequest;
import com.sustajn.oderservice.request.LeasedReturnedGraphInput;
import com.sustajn.oderservice.request.ReturnRequest;
import com.sustajn.oderservice.service.OrderService;
import com.sustajn.oderservice.service.impl.OrderNotificationService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * Borrow containers
     */
        @PostMapping("/borrowContainers")
    public ResponseEntity<Map<String, Object>> borrowContainers(
            @RequestBody BorrowRequest request
    ) {
        Map<String, Object> response = orderService.borrowContainers(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Return containers
     */
    @PostMapping("/returnContainers")
    public ResponseEntity<Map<String, Object>> returnContainers(
            @RequestBody ReturnRequest request
    ) {
        Map<String, Object> response = orderService.returnContainers(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/borrowed/{userId}")
    public ResponseEntity<Map<String, Object>> getBorrowedContainersForUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "APPROVED") String status) {

        Map<String, Object> response = orderService
                .getOrderDetailsListByStatusForUser(userId, status);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/monthWiseBorrowedDetails")
    public ResponseEntity<Map<String, Object>> getMonthWiseOrders(
            @RequestParam Long userId,
            @RequestParam int year) {
        Map<String, Object> response = orderService.getMonthWiseOrders(userId, year);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/details/{orderId}")
    public ResponseEntity<?> getOrderDetailsByOrderId(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getOrderDetailsByOrderId(orderId));
    }


    @PostMapping("/approve/{orderId}")
    public ResponseEntity<Map<String,Object>> approveOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.approveOrder(orderId));
    }


    @GetMapping("/monthWiseReturnedDetails")
    public ResponseEntity<Map<String, Object>> getMonthWiseReturnOrders(
            @RequestParam Long userId,
            @RequestParam int year) {
        Map<String, Object> response = orderService.getMonthWiseReturnOrders(userId, year);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/productsSummary/{userId}")
    public ResponseEntity<?> getBorrowSummary(@PathVariable Long userId) {
        return ResponseEntity.ok(orderService.getBorrowedProductSummary(userId));
    }

    @GetMapping("/orderHistory/{restaurantId}")
    public ResponseEntity<?> getOrderHistory(@PathVariable @NotNull(message = "please provide resturant id") Long restaurantId) {
        return ResponseEntity.ok(orderService.getOrderHistory(restaurantId));
    }

    @GetMapping("/getLeasedReturnedCount")
    public ResponseEntity<?> getLeasedAndReturnedContainersCount(
            @RequestParam @NotNull(message = "please provide restaurant id") Long restaurantId,
            @RequestParam @NotNull(message = "please provide product id") Integer productId) {
        return ResponseEntity.ok(orderService.getLeasedAndReturnedContainersCount(restaurantId, productId));
    }

    @GetMapping("/getLeasedReturnedMonthYearDetails")
    public ResponseEntity<?> getLeasedReturnedMonthYearDetails(@RequestBody @Validated LeasedReturnedGraphInput leasedReturnedGraphInput
    ) {
        return ResponseEntity.ok(orderService.getLeasedReturnedMonthYearDetails(leasedReturnedGraphInput));
    }

    @GetMapping("/getLeasedReturnedCountWithTimeGraph")
    public ResponseEntity<?> getLeasedReturnedCountWithTimeGraph(@RequestBody @Validated LeasedReturnedGraphInput leasedReturnedGraphInput
    ) {
        return ResponseEntity.ok(orderService.getLeasedReturnedCountWithTimeGraph(leasedReturnedGraphInput));
    }

    private final OrderNotificationService borrowOrderService;

    /**
     * Extend all borrow orders of a given order by 5 days.
     */
    @PutMapping("/{orderId}/extend")
    public ResponseEntity<ApiResponse<Integer>> extendOrder(@PathVariable Long orderId) {

        // Returns number of items extended
         borrowOrderService.extendBorrowOrder(orderId);
        return ResponseEntity.ok(
                new ApiResponse<>("success",
                        "Order extended by 5 days successfully",
                        null)
        );
    }
}
