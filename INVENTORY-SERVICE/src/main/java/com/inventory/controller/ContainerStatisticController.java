package com.inventory.controller;

import com.inventory.dto.ContainerDetailsResponse;
import com.inventory.service.impl.ContainerDashboardServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/inventory/cointainerstatistic")
@RequiredArgsConstructor
public class ContainerStatisticController {

    private final ContainerDashboardServiceImpl dashboardService;

    @GetMapping("/details/{containerTypeId}")
    public ResponseEntity<Map<String, Object>> getDetails(@PathVariable Integer containerTypeId) {
        ContainerDetailsResponse details = dashboardService.getContainerDetails(containerTypeId);

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Data fetched successfully",
                "data", details
        ));
    }
}