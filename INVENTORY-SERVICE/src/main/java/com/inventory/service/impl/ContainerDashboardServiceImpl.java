package com.inventory.service.impl;

import com.inventory.dto.ContainerDetailsResponse;
import com.inventory.entity.AdminInventoryMaster;
import com.inventory.entity.ContainerType;
import com.inventory.feignClient.OrderFeignClient;
import com.inventory.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContainerDashboardServiceImpl {

    private final ContainerTypeRepository containerTypeRepository;
    private final AdminInventoryMasterRepository adminInventoryMasterRepository;
    private final AdminRestaurantInventoryDetailsRepository adminDetailsRepository;
    private final RestaurantInventoryMasterRepository restaurantInventoryRepository;
    private final DamagedContainerRepository damagedContainerRepository;
    private final OrderFeignClient orderFeignClient;

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
}