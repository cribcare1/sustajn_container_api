package com.inventory.controller;

import com.inventory.Constant.TransactionType;
import com.inventory.dto.ContainerInventoryStatsDto;
import com.inventory.entity.AdminInventoryMaster;
import com.inventory.entity.ContainerType;
import com.inventory.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    private final AdminOrderItemRepository adminOrderItemRepository; // 🟢 Injected

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
}