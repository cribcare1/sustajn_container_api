package com.inventory.controller;

import com.inventory.repository.DamagedContainerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/inventory/damaged")
@RequiredArgsConstructor
public class DamagedContainerController {

    private final DamagedContainerRepository damagedContainerRepository;

    @GetMapping("/count")
    public Integer getDamagedCount(
            @RequestParam("restaurantId") Long restaurantId,
            @RequestParam(value = "containerTypeId", required = false) Integer containerTypeId,
            @RequestParam(value = "month", required = false) Integer month,
            @RequestParam(value = "year", required = false) Integer year
    ) {
        LocalDateTime startDate = null;
        LocalDateTime endDate = null;

        if (month != null && year != null) {
            startDate = LocalDateTime.of(year, month, 1, 0, 0);
            endDate = startDate.plusMonths(1).minusSeconds(1);
        }

        Integer count = damagedContainerRepository.countDamagedContainers(
                restaurantId, containerTypeId, startDate, endDate);

        return count != null ? count : 0;
    }
}