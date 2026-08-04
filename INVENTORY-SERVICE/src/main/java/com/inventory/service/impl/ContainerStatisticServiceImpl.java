package com.inventory.service.impl;

import com.inventory.dto.*;
import com.inventory.entity.AdminInventoryMaster;
import com.inventory.entity.ContainerType;
import com.inventory.feignClient.AuthFeignClient;
import com.inventory.feignClient.OrderFeignClient;
import com.inventory.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.inventory.dto.ProductWithPartnerDetailResponse;
import com.inventory.dto.PartnerHoldingDto;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContainerStatisticServiceImpl {

    private final ContainerTypeRepository containerTypeRepository;
    private final AdminInventoryMasterRepository adminInventoryMasterRepository;
    private final AdminRestaurantInventoryDetailsRepository adminDetailsRepository;
    private final RestaurantInventoryMasterRepository restaurantInventoryRepository;
    private final DamagedContainerRepository damagedContainerRepository;
    private final OrderFeignClient orderFeignClient;
    private final AuthFeignClient authFeignClient;
    public ContainerDetailsResponse getContainerDetails(Integer containerTypeId) {

        // 1. Base Info
        ContainerType container = containerTypeRepository.findById(containerTypeId)
                .orElseThrow(() -> new RuntimeException("Container not found"));

        // 2. Admin Warehouse Stock (Ordered & In-Stock)
        AdminInventoryMaster adminStock = adminInventoryMasterRepository.findByContainerTypeId(containerTypeId).orElse(null);
        Integer ordered = (adminStock != null && adminStock.getTotalContainers() != null) ? adminStock.getTotalContainers() : 0;
        Integer inStock = (adminStock != null && adminStock.getAvailableContainers() != null) ? adminStock.getAvailableContainers() : 0;

        // 3. Admin <-> Partner Transactions
        Integer issued = adminDetailsRepository.getTotalByActionType(containerTypeId, "BORROW");
        Integer returned = adminDetailsRepository.getTotalByActionType(containerTypeId, "RETURN");

        // 4. Partner Current Stock
        Integer withPartner = restaurantInventoryRepository.getTotalWithPartner(containerTypeId);

        // 5. Damaged
        Integer damaged = damagedContainerRepository.countTotalDamaged(containerTypeId);

        // 6. In Circulation (Fetch from Order Service via Feign)
        Integer inCirculation = 0;
        try {
            Long productIdLong = Long.parseLong(container.getProductId());
            inCirculation = orderFeignClient.getInCirculationCount(productIdLong);
        } catch (Exception e) {
            log.error("Failed to fetch circulation count from Order Service: {}", e.getMessage());
        }

        // 7. Sold (Mocked as 0 for now until you create a Sold table)
        Integer sold = 0;

        return ContainerDetailsResponse.builder()
                .containerTypeId(containerTypeId)
                .name(container.getName())
                .productId(container.getProductId())
                .capacity(container.getCapacityMl() != null ? container.getCapacityMl() + "ml" : "")
                .imageUrl(container.getImageUrl())
                .ordered(ordered)
                .issuedToPartner(issued)
                .inCirculation(inCirculation)
                .withPartner(withPartner)
                .sold(sold)
                .damaged(damaged)
                .inStock(inStock)
                .returned(returned)
                .build();
    }

    public List<ProductCirculationResponse> getAllProductsInCirculation() {

        // 1. Fetch all active container types
        List<ContainerType> activeContainers = containerTypeRepository.findAll()
                .stream()
                .filter(c -> "active".equalsIgnoreCase(c.getStatus()))
                .collect(Collectors.toList());

        // 2. Bulk fetch all circulation counts from Order Service
        Map<Long, Integer> circulationMap = new HashMap<>();
        try {
            circulationMap = orderFeignClient.getBulkCirculationCounts();
        } catch (Exception e) {
            log.error("Failed to fetch bulk circulation counts from Order Service: {}", e.getMessage());
        }

        // 3. Map the counts to the containers
        Map<Long, Integer> finalCirculationMap = circulationMap; // For use in lambda

        return activeContainers.stream().map(container -> {
            // Map the container ID (Integer) to the Order Service productId (Long)
            Long pId = container.getId().longValue();
            Integer count = finalCirculationMap.getOrDefault(pId, 0);

            return ProductCirculationResponse.builder()
                    .containerTypeId(container.getId())
                    .name(container.getName())
                    .productId(container.getProductId())
                    .capacity(container.getCapacityMl() != null ? container.getCapacityMl() + "ml" : "")
                    .imageUrl(container.getImageUrl())
                    .inCirculationCount(count)
                    .build();
        }).collect(Collectors.toList());
    }

    // Make sure you inject this at the top!
    // private final AuthFeignClient authFeignClient;

    public ContainerInCirculationDetailResponse getInCirculationDetails(Integer containerTypeId) {

        // 1. Get Product Info
        ContainerType container = containerTypeRepository.findById(containerTypeId)
                .orElseThrow(() -> new RuntimeException("Container not found"));
        Long productIdLong = container.getId().longValue();

        // 2. Fetch User Holding Breakdown (Isolated try-catch)
        Map<Long, Integer> numericUserMap = new HashMap<>();
        try {
            numericUserMap = orderFeignClient.getCirculationByUser(productIdLong);
        } catch (Exception e) {
            log.error("Failed to fetch user circulation map for productId {}: {}", productIdLong, e.getMessage());
        }

        // 3. Fetch Total Count (Isolated try-catch + fallback to user map sum)
        Integer totalInCirculation = 0;
        try {
            totalInCirculation = orderFeignClient.getInCirculationCount(productIdLong);
        } catch (Exception e) {
            log.error("Failed to fetch circulation count for productId {}: {}", productIdLong, e.getMessage());
            // Fallback: sum counts from user map if available
            totalInCirculation = numericUserMap.values().stream().mapToInt(Integer::intValue).sum();
        }

        // 4. Translate User IDs via Auth Service
        Map<Long, String> customerIdMap = new HashMap<>();
        if (!numericUserMap.isEmpty()) {
            try {
                List<Long> numericIds = new ArrayList<>(numericUserMap.keySet());
                customerIdMap = authFeignClient.getCustomerIdsBulk(numericIds);
            } catch (Exception e) {
                log.error("Failed to fetch auth customer IDs: {}", e.getMessage());
            }
        }

        // 5. Build and Sort User List
        Map<Long, String> finalCustomerIdMap = customerIdMap;
        List<UserHoldingDto> userList = numericUserMap.entrySet().stream()
                .map(entry -> {
                    String displayId = finalCustomerIdMap.getOrDefault(entry.getKey(), "USER-" + entry.getKey());
                    return UserHoldingDto.builder()
                            .userId(displayId)
                            .count(entry.getValue())
                            .build();
                })
                .sorted((a, b) -> b.getCount().compareTo(a.getCount()))
                .collect(Collectors.toList());

        // 6. Return Details
        return ContainerInCirculationDetailResponse.builder()
                .containerTypeId(containerTypeId)
                .name(container.getName())
                .productId(container.getProductId())
                .capacity(container.getCapacityMl() != null ? container.getCapacityMl() + "ml" : "")
                .imageUrl(container.getImageUrl())
                .totalInCirculation(totalInCirculation)
                .users(userList)
                .build();
    }

    // ... Inside ContainerStatisticServiceImpl ...

    public List<ProductWithPartnerResponse> getAllProductsWithPartner() {

        // 1. Fetch all active container types (The base products)
        List<ContainerType> activeContainers = containerTypeRepository.findAll()
                .stream()
                .filter(c -> "active".equalsIgnoreCase(c.getStatus()))
                .collect(Collectors.toList());

        // 2. Fetch the grouped counts from the database
        List<Object[]> results = restaurantInventoryRepository.getWithPartnerCountsForAllProducts();

        // Convert to a fast lookup map: { containerTypeId : count }
        Map<Integer, Integer> countsMap = new HashMap<>();
        for (Object[] row : results) {
            Integer cId = (Integer) row[0];
            Integer count = ((Number) row[1]).intValue();
            countsMap.put(cId, count);
        }

        // 3. Map the counts to the containers
        return activeContainers.stream().map(container -> {

            // Look up the count, default to 0 if no partner has it
            Integer count = countsMap.getOrDefault(container.getId(), 0);

            return ProductWithPartnerResponse.builder()
                    .containerTypeId(container.getId())
                    .name(container.getName())
                    .productId(container.getProductId())
                    .capacity(container.getCapacityMl() != null ? container.getCapacityMl() + "ml" : "")
                    .imageUrl(container.getImageUrl())
                    .withPartnerCount(count)
                    .build();

        }).collect(Collectors.toList());
    }
    public ProductWithPartnerDetailResponse getWithPartnerDetails(Integer containerTypeId) {
        ContainerType container = containerTypeRepository.findById(containerTypeId)
                .orElseThrow(() -> new RuntimeException("Container not found"));

        List<Object[]> results = restaurantInventoryRepository.getPartnerHoldingsByContainerType(containerTypeId);

        Integer totalWithPartner = 0;
        List<Long> restaurantIdsToFetch = new ArrayList<>();
        Map<Long, Integer> countsMap = new HashMap<>();

        for (Object[] row : results) {
            Long restaurantId = (Long) row[0];
            Integer count = ((Number) row[1]).intValue();

            totalWithPartner += count;
            restaurantIdsToFetch.add(restaurantId);
            countsMap.put(restaurantId, count);
        }

        // 4. Fetch Real Names and Addresses from Auth Service via Feign
        Map<Long, PartnerInfoDto> partnerInfoMap = new HashMap<>();
        if (!restaurantIdsToFetch.isEmpty()) {
            try {
                partnerInfoMap = authFeignClient.getPartnerDetailsBulk(restaurantIdsToFetch);
            } catch (Exception e) {
                log.error("Failed to fetch partner details from Auth Service: {}", e.getMessage());
            }
        }

        // 5. Build the final list using the real Auth data
        List<PartnerHoldingDto> partnerList = new ArrayList<>();
        for (Long restaurantId : restaurantIdsToFetch) {
            PartnerInfoDto info = partnerInfoMap.get(restaurantId);

            // Apply the real name and address!
            String partnerName = (info != null && info.getName() != null) ? info.getName() : "Partner " + restaurantId;
            String address = (info != null && info.getAddress() != null) ? info.getAddress() : "Address unavailable";

            partnerList.add(PartnerHoldingDto.builder()
                    .restaurantId(restaurantId)
                    .partnerName(partnerName)
                    .address(address)
                    .count(countsMap.get(restaurantId))
                    .build());
        }

        partnerList.sort((a, b) -> b.getCount().compareTo(a.getCount()));

        return ProductWithPartnerDetailResponse.builder()
                .containerTypeId(containerTypeId)
                .name(container.getName())
                .productId(container.getProductId())
                .capacity(container.getCapacityMl() != null ? container.getCapacityMl() + "ml" : "")
                .imageUrl(container.getImageUrl())
                .totalWithPartner(totalWithPartner)
                .partners(partnerList)
                .build();
    }
}