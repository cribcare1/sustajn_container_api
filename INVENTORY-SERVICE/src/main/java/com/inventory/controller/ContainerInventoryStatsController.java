package com.inventory.controller;

import com.inventory.Constant.TransactionType;
import com.inventory.dto.*;
import com.inventory.entity.AdminInventoryMaster;
import com.inventory.entity.ContainerType;
import com.inventory.feignClient.AuthFeignClient;
import com.inventory.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

import static org.apache.http.client.utils.DateUtils.formatDate;
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

    @GetMapping("/returned-history/{containerTypeId}")
    public ResponseEntity<List<ReturnedMonthResponse>> getReturnedHistory(
            @PathVariable Integer containerTypeId,
            @RequestParam(required = false) String searchKeyword) {

        List<Object[]> rawRows = adminOrderItemRepository.findReturnedDetailsByContainerType(containerTypeId);

        if (CollectionUtils.isEmpty(rawRows)) {
            return ResponseEntity.ok(List.of());
        }

        // 1. Extract distinct restaurant IDs
        List<Long> restaurantIds = rawRows.stream()
                .map(r -> r[1] != null ? ((Number) r[1]).longValue() : null)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // 2. Fetch Partner/Restaurant details in bulk via AuthFeignClient
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

        // 3. Group rows chronologically by month using collectedOnDate (r[5]) or returnedOnDate (r[4])
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

        List<ReturnedMonthResponse> responseList = new ArrayList<>();

        for (Map.Entry<String, List<Object[]>> entry : monthMap.entrySet()) {
            String monthYear = entry.getKey();
            List<Object[]> rows = entry.getValue();

            List<ReturnedDetailResponse> detailList = new ArrayList<>();

            for (Object[] r : rows) {
                Long orderId = r[0] != null ? ((Number) r[0]).longValue() : null;
                Long restaurantId = r[1] != null ? ((Number) r[1]).longValue() : null;
                String containerCode = r[2] != null ? (String) r[2] : "";
                Integer qty = r[3] != null ? ((Number) r[3]).intValue() : 0;

                // Format both Returned On and Collected On dates using custom helper
                String returnedOnStr = formatDateHelper(r[4], dateFormatter);
                String collectedOnStr = formatDateHelper(r[5] != null ? r[5] : r[4], dateFormatter);

                PartnerInfoDto partnerInfo = finalPartnerMap.get(restaurantId);
                String restaurantName = partnerInfo != null && partnerInfo.getName() != null
                        ? partnerInfo.getName() : "Partner #" + restaurantId;
                String restaurantAddress = partnerInfo != null && partnerInfo.getAddress() != null
                        ? partnerInfo.getAddress() : "";

                // Search keyword filter
                if (StringUtils.hasText(searchKeyword)) {
                    if (!restaurantName.toLowerCase().contains(searchKeyword.toLowerCase())) {
                        continue;
                    }
                }

                detailList.add(ReturnedDetailResponse.builder()
                        .orderId(orderId)
                        .restaurantId(restaurantId)
                        .restaurantName(restaurantName)
                        .restaurantAddress(restaurantAddress)
                        .containerCode(containerCode)
                        .quantity(qty)
                        .returnedOn(returnedOnStr)
                        .collectedOn(collectedOnStr)
                        .build());
            }

            if (!detailList.isEmpty()) {
                int monthTotal = detailList.stream().mapToInt(ReturnedDetailResponse::getQuantity).sum();
                responseList.add(ReturnedMonthResponse.builder()
                        .monthYear(monthYear)
                        .totalQuantity(monthTotal)
                        .returns(detailList)
                        .build());
            }
        }

        return ResponseEntity.ok(responseList);
    }

    // 🟢 HELPER 1: Safe Date Object parsing to LocalDate
    private LocalDate parseLocalDate(Object dateObj) {
        if (dateObj == null) return LocalDate.now();
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

    // 🟢 HELPER 2: Safe Date formatting helper method
    private String formatDateHelper(Object dateObj, DateTimeFormatter formatter) {
        if (dateObj == null) return "";
        return parseLocalDate(dateObj).format(formatter);
    }
}