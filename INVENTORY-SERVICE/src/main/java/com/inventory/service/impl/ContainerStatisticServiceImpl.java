package com.inventory.service.impl;

import com.inventory.dto.ContainerDetailsResponse;
import com.inventory.entity.AdminInventoryMaster;
import com.inventory.entity.ContainerType;
import com.inventory.feignClient.AuthFeignClient;
import com.inventory.feignClient.OrderFeignClient;
import com.inventory.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.inventory.dto.ProductCirculationResponse;
import com.inventory.dto.ContainerInCirculationDetailResponse;
import com.inventory.dto.UserHoldingDto;
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
        // 2. Get counts from Order Service
        Integer totalInCirculation = 0;
        Map<Long, Integer> numericUserMap = new HashMap<>();
        try {
            totalInCirculation = orderFeignClient.getInCirculationCount(productIdLong);
            numericUserMap = orderFeignClient.getCirculationByUser(productIdLong);
        } catch (Exception e) {
            log.error("Failed to fetch order data: {}", e.getMessage());
        }

        // 3. Translate IDs using Auth Service
        Map<Long, String> customerIdMap = new HashMap<>();
        if (!numericUserMap.isEmpty()) {
            try {
                List<Long> numericIds = new java.util.ArrayList<>(numericUserMap.keySet());
                customerIdMap = authFeignClient.getCustomerIdsBulk(numericIds);
            } catch (Exception e) {
                log.error("Failed to fetch auth data: {}", e.getMessage());
            }
        }

        // 4. Build and Sort the User List
        Map<Long, String> finalCustomerIdMap = customerIdMap;
        List<UserHoldingDto> userList = numericUserMap.entrySet().stream()
                .map(entry -> {
                    String displayId = finalCustomerIdMap.getOrDefault(entry.getKey(), "USER-" + entry.getKey());
                    return UserHoldingDto.builder()
                            .userId(displayId)
                            .count(entry.getValue())
                            .build();
                })
                .sorted((a, b) -> b.getCount().compareTo(a.getCount())) // Sort highest count first
                .collect(java.util.stream.Collectors.toList());

        // 5. Return Final Response
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
}