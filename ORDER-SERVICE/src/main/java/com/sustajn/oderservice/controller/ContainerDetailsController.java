package com.sustajn.oderservice.controller;

import com.sustajn.oderservice.dto.ContainerDetailsResponse;
import com.sustajn.oderservice.dto.MonthlyOrderedResponse;
import com.sustajn.oderservice.feign.service.InventoryFeignClient;
import com.sustajn.oderservice.service.ContainerDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders/containers")
@RequiredArgsConstructor
public class ContainerDetailsController {

    private final ContainerDetailsService containerDetailsService;
    private final InventoryFeignClient inventoryFeignClient;

    @GetMapping("/{productId}/details")
    public ResponseEntity<ContainerDetailsResponse> getContainerDetails(@PathVariable Long productId) {
        return ResponseEntity.ok(containerDetailsService.getContainerDetails(productId));
    }

    @GetMapping("/ordered-history/{productId}")
    public ResponseEntity<List<MonthlyOrderedResponse>> getOrderedHistory(@PathVariable Long productId) {
        List<MonthlyOrderedResponse> history = inventoryFeignClient.getOrderedHistory(productId.intValue());
        return ResponseEntity.ok(history != null ? history : List.of());
    }
}