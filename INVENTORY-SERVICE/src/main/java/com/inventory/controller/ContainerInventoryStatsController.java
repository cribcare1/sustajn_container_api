package com.inventory.controller;

import com.inventory.Constant.TransactionType;
import com.inventory.dto.*;
import com.inventory.entity.AdminInventoryMaster;
import com.inventory.entity.ContainerType;
import com.inventory.feignClient.AuthFeignClient;
import com.inventory.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static org.hibernate.query.sqm.tree.SqmNode.log;

@RestController
@RequestMapping("/inventory/containers")
@RequiredArgsConstructor
@Slf4j
public class ContainerInventoryStatsController {

    private final ContainerTypeRepository containerTypeRepository;
    private final AdminInventoryMasterRepository adminInventoryMasterRepository;
    private final AdminRestaurantInventoryDetailsRepository adminRestaurantInventoryDetailsRepository;
    private final RestaurantContainerInventoryRepository restaurantContainerInventoryRepository;
    private final DamagedContainerRepository damagedContainerRepository;
    private final SoldContainerRepository soldContainerRepository;
    private final AdminOrderItemRepository adminOrderItemRepository;
    private final AdminOrderRepository adminOrderRepository;
    private final AuthFeignClient authFeignClient;

    @GetMapping("/{containerTypeId}/stats")
    public ResponseEntity<ContainerInventoryStatsDto> getContainerInventoryStats(@PathVariable Integer containerTypeId) {

        ContainerType type = containerTypeRepository.findById(containerTypeId)
                .orElse(new ContainerType());

        AdminInventoryMaster master = adminInventoryMasterRepository.findByContainerTypeId(containerTypeId)
                .orElse(null);

        int orderedCount = adminOrderItemRepository.sumQtyByContainerTypeIdAndType(containerTypeId, TransactionType.BORROW);

        // 🟢 ACCURATE COUNT: Sum of approvedQty from admin_order_items for APPROVED BORROW orders
        int issuedToPartnerCount = adminOrderItemRepository.sumIssuedToPartnerCountByContainerTypeId(containerTypeId);

        int inStockCount = master != null ? master.getAvailableContainers() : 0;
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
                .issuedToPartnerCount(issuedToPartnerCount) // 🟢 Accurate count sent over Feign
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

    @GetMapping("/issued-to-partner/{containerTypeId}")
    public ResponseEntity<List<IssuedToPartnerMonthResponse>> getIssuedToPartnerHistory(@PathVariable Integer containerTypeId) {
        List<Object[]> rawRows = adminOrderItemRepository.findIssuedDetailsByContainerType(containerTypeId);

        if (CollectionUtils.isEmpty(rawRows)) {
            return ResponseEntity.ok(List.of());
        }

        // 1. Extract distinct restaurant IDs
        List<Long> restaurantIds = rawRows.stream()
                .map(r -> r[1] != null ? ((Number) r[1]).longValue() : null)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // 2. Fetch Partner/Restaurant details in a single bulk call via AuthFeignClient
        Map<Long, PartnerInfoDto> partnerDetailsMap = Collections.emptyMap();
        try {
            if (!restaurantIds.isEmpty()) {
                partnerDetailsMap = authFeignClient.getPartnerDetailsBulk(restaurantIds);
            }
        } catch (Exception ex) {
            log.error("Error fetching partner details from AUTH-SERVICE via Feign: {}", ex.getMessage());
        }

        final Map<Long, PartnerInfoDto> finalPartnerMap = partnerDetailsMap != null ? partnerDetailsMap : Collections.emptyMap();

        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMMM-yyyy", Locale.ENGLISH);
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        // 3. Group rows by month using deliveredDate (r[5]) or orderDate (r[4])
        Map<String, List<Object[]>> monthMap = rawRows.stream()
                .filter(r -> r[5] != null || r[4] != null)
                .collect(Collectors.groupingBy(
                        r -> {
                            Object dateObj = r[5] != null ? r[5] : r[4];
                            LocalDate date = parseLocalDate(dateObj);
                            return date.format(monthFormatter);
                        },
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<IssuedToPartnerMonthResponse> responseList = new ArrayList<>();

        for (Map.Entry<String, List<Object[]>> entry : monthMap.entrySet()) {
            String monthYear = entry.getKey();
            List<Object[]> rows = entry.getValue();

            int monthTotal = rows.stream()
                    .mapToInt(r -> r[3] != null ? ((Number) r[3]).intValue() : 0)
                    .sum();

            List<PartnerIssuedDetailResponse> detailList = rows.stream().map(r -> {
                Long orderId = r[0] != null ? ((Number) r[0]).longValue() : null;
                Long restaurantId = r[1] != null ? ((Number) r[1]).longValue() : null;
                String containerCode = r[2] != null ? (String) r[2] : "";
                Integer qty = r[3] != null ? ((Number) r[3]).intValue() : 0;

                String orderedDateStr = formatDate(r[4], dateFormatter);
                String deliveredDateStr = formatDate(r[5], dateFormatter);

                // Populate Name and Address from Feign Map
                PartnerInfoDto partnerInfo = finalPartnerMap.get(restaurantId);
                String restaurantName = partnerInfo != null && partnerInfo.getName() != null
                        ? partnerInfo.getName() : "Partner #" + restaurantId;
                String restaurantAddress = partnerInfo != null && partnerInfo.getAddress() != null
                        ? partnerInfo.getAddress() : "";

                return PartnerIssuedDetailResponse.builder()
                        .orderId(orderId)
                        .restaurantId(restaurantId)
                        .restaurantName(restaurantName)
                        .restaurantAddress(restaurantAddress)
                        .containerCode(containerCode)
                        .quantity(qty)
                        .orderedDate(orderedDateStr)
                        .deliveredDate(deliveredDateStr)
                        .build();
            }).collect(Collectors.toList());

            responseList.add(IssuedToPartnerMonthResponse.builder()
                    .monthYear(monthYear)
                    .totalQuantity(monthTotal)
                    .issuances(detailList)
                    .build());
        }

        return ResponseEntity.ok(responseList);
    }

    private LocalDate parseLocalDate(Object dateObj) {
        if (dateObj instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        } else if (dateObj instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime().toLocalDate();
        } else if (dateObj instanceof LocalDateTime localDateTime) {
            return localDateTime.toLocalDate();
        } else if (dateObj instanceof LocalDate localDate) {
            return localDate;
        }
        return LocalDate.now();
    }

    private String formatDate(Object dateObj, DateTimeFormatter formatter) {
        if (dateObj == null) return "";
        return parseLocalDate(dateObj).format(formatter);
    }
}