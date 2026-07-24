package com.inventory.controller;

import com.inventory.Constant.TransactionType;
import com.inventory.dto.ContainerInventoryStatsDto;
import com.inventory.dto.MonthlyOrderedResponse;
import com.inventory.entity.AdminInventoryMaster;
import com.inventory.entity.ContainerType;
import com.inventory.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/inventory/containers")
@RequiredArgsConstructor
public class ContainerInventoryStatsController {

    private final ContainerTypeRepository containerTypeRepository;
    private final AdminInventoryMasterRepository adminInventoryMasterRepository;
    private final AdminRestaurantInventoryDetailsRepository adminRestaurantInventoryDetailsRepository;
    private final RestaurantContainerInventoryRepository restaurantContainerInventoryRepository;
    private final DamagedContainerRepository damagedContainerRepository;
    private final SoldContainerRepository soldContainerRepository;
    private final AdminOrderItemRepository adminOrderItemRepository;
    private final AdminOrderRepository adminOrderRepository;

    @GetMapping("/{containerTypeId}/stats")
    public ResponseEntity<ContainerInventoryStatsDto> getContainerInventoryStats(@PathVariable Integer containerTypeId) {

        ContainerType type = containerTypeRepository.findById(containerTypeId)
                .orElse(new ContainerType());

        AdminInventoryMaster master = adminInventoryMasterRepository.findByContainerTypeId(containerTypeId)
                .orElse(null);

        // 🟢 ORDERED COUNT: Fetch from admin_orders JOIN admin_order_items where type = BORROW
        int orderedCount = adminOrderItemRepository.sumQtyByContainerTypeIdAndType(containerTypeId, TransactionType.BORROW);

        int inStockCount = master != null ? master.getAvailableContainers() : 0;
        int issuedToPartnerCount = adminRestaurantInventoryDetailsRepository.sumIssuedToPartnerCount(containerTypeId);
        int withPartnerCount = restaurantContainerInventoryRepository.sumWithPartnerCount(containerTypeId);
        int damagedCount = damagedContainerRepository.sumDamagedCountByContainerTypeId(containerTypeId);
        int soldCount = soldContainerRepository.sumSoldCountByContainerTypeId(containerTypeId);

        return ResponseEntity.ok(ContainerInventoryStatsDto.builder()
                .containerTypeId(containerTypeId)
                .name(type.getName())
                .productCode(type.getProductId())
                .capacity(type.getCapacityMl() != null ? type.getCapacityMl() + "ml" : "")
                .imageUrl(type.getImageUrl())
                .orderedCount(orderedCount)
                .issuedToPartnerCount(issuedToPartnerCount)
                .withPartnerCount(withPartnerCount)
                .soldCount(soldCount)
                .damagedCount(damagedCount)
                .inStockCount(inStockCount)
                .build());
    }

    @GetMapping("/{containerTypeId}/ordered-history")
    public ResponseEntity<List<MonthlyOrderedResponse>> getOrderedHistory(@PathVariable Integer containerTypeId) {
        List<Object[]> rawRows = adminOrderRepository.findBorrowOrdersRawByContainerTypeId(containerTypeId);

        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMMM-yyyy", Locale.ENGLISH);
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        // Use LinkedHashMap to keep order
        Map<String, Map<String, Integer>> groupedMap = new LinkedHashMap<>();

        for (Object[] row : rawRows) {
            if (row[0] == null) continue;

            LocalDateTime orderDate = (LocalDateTime) row[0];
            int qty = row[1] != null ? ((Number) row[1]).intValue() : 0;

            String monthKey = orderDate.format(monthFormatter); // e.g., "November-2025"
            String dateKey = orderDate.format(dateFormatter);   // e.g., "25.11.2025"

            groupedMap.putIfAbsent(monthKey, new LinkedHashMap<>());
            Map<String, Integer> dailyMap = groupedMap.get(monthKey);
            dailyMap.put(dateKey, dailyMap.getOrDefault(dateKey, 0) + qty);
        }

        List<MonthlyOrderedResponse> response = new ArrayList<>();
        for (Map.Entry<String, Map<String, Integer>> monthEntry : groupedMap.entrySet()) {
            String monthYear = monthEntry.getKey();
            Map<String, Integer> dailyMap = monthEntry.getValue();

            int monthTotal = 0;
            List<MonthlyOrderedResponse.DailyOrderedItem> dailyItems = new ArrayList<>();

            for (Map.Entry<String, Integer> dayEntry : dailyMap.entrySet()) {
                int dayQty = dayEntry.getValue();
                monthTotal += dayQty;
                dailyItems.add(new MonthlyOrderedResponse.DailyOrderedItem(dayEntry.getKey(), dayQty));
            }

            response.add(MonthlyOrderedResponse.builder()
                    .monthYear(monthYear)
                    .monthTotal(monthTotal)
                    .dailyOrders(dailyItems)
                    .build());
        }

        return ResponseEntity.ok(response);
    }
}