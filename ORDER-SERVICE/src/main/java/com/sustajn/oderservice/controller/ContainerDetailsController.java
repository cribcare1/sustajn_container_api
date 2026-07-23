package com.sustajn.oderservice.controller;

import com.sustajn.oderservice.dto.ContainerDetailsResponse;
import com.sustajn.oderservice.service.ContainerDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders/containers")
@RequiredArgsConstructor
public class ContainerDetailsController {

    private final ContainerDetailsService containerDetailsService;

    @GetMapping("/{productId}/details")
    public ResponseEntity<ContainerDetailsResponse> getContainerDetails(@PathVariable Long productId) {
        return ResponseEntity.ok(containerDetailsService.getContainerDetails(productId));
    }
}