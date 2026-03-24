package com.inventory.controller;

import com.inventory.dto.ContainerDetailsResponse;
import com.inventory.dto.ContainerInCirculationDetailResponse;
import com.inventory.dto.ProductCirculationResponse;
import com.inventory.service.impl.ContainerStatisticServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/inventory/cointainerstatistic")
@RequiredArgsConstructor
public class ContainerStatisticController {

    private final ContainerStatisticServiceImpl dashboardService;

    @GetMapping("/details/{containerTypeId}")
    public ResponseEntity<Map<String, Object>> getDetails(@PathVariable Integer containerTypeId) {
        ContainerDetailsResponse details = dashboardService.getContainerDetails(containerTypeId);

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Data fetched successfully",
                "data", details
        ));
    }
    @GetMapping("/in-circulation-list")
    public ResponseEntity<Map<String, Object>> getInCirculationList() {
        List<ProductCirculationResponse> list = dashboardService.getAllProductsInCirculation();

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "In Circulation list fetched successfully",
                "data", list
        ));
    }
    @GetMapping("/in-circulation/{containerTypeId}")
    public ResponseEntity<Map<String, Object>> getInCirculationDetails(@PathVariable Integer containerTypeId) {
        ContainerInCirculationDetailResponse details = dashboardService.getInCirculationDetails(containerTypeId);

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "In Circulation details fetched successfully",
                "data", details
        ));
    }
}