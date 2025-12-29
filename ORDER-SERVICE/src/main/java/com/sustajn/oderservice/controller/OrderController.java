package com.sustajn.oderservice.controller;

import com.sustajn.oderservice.request.BorrowRequest;
import com.sustajn.oderservice.request.ReturnRequest;
import com.sustajn.oderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
}
