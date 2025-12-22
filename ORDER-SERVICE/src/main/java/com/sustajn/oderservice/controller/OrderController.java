package com.sustajn.oderservice.controller;

import com.sustajn.oderservice.request.BorrowRequest;
import com.sustajn.oderservice.request.ReturnRequest;
import com.sustajn.oderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
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
}
