package com.sustajn.oderservice.service.impl;

import com.sustajn.oderservice.dto.ContainerDetailsResponse;
import com.sustajn.oderservice.dto.ContainerInventoryStatsDto;
import com.sustajn.oderservice.feign.service.InventoryFeignClient;
import com.sustajn.oderservice.repository.BorrowOrderRepository;
import com.sustajn.oderservice.repository.ReturnOrderRepository;
import com.sustajn.oderservice.service.ContainerDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContainerDetailsServiceImpl implements ContainerDetailsService {

    private final BorrowOrderRepository borrowOrderRepository;
    private final ReturnOrderRepository returnOrderRepository;
    private final InventoryFeignClient inventoryFeignClient;

    @Override
    public ContainerDetailsResponse getContainerDetails(Long productId) {


        ContainerInventoryStatsDto invStats = null;
        try {
            invStats = inventoryFeignClient.getContainerInventoryStats(productId.intValue());
        } catch (Exception ex) {
            log.error("Failed to fetch inventory stats over Feign for productId {}: ", productId, ex);
        }


        int inCirculationCount = borrowOrderRepository.sumInCirculationByProductId(productId);
        int returnedCount = returnOrderRepository.sumReturnedCountByProductId(productId);


        int orderedCount = invStats != null ? invStats.getOrderedCount() : 0;
        int issuedToPartnerCount = invStats != null ? invStats.getIssuedToPartnerCount() : 0;
        int withPartnerCount = invStats != null ? invStats.getWithPartnerCount() : 0;
        int soldCount = invStats != null ? invStats.getSoldCount() : 0;
        int damagedCount = invStats != null ? invStats.getDamagedCount() : 0;
        int inStockCount = invStats != null ? invStats.getInStockCount() : 0;


        return ContainerDetailsResponse.builder()
                .productId(productId)
                .name(invStats != null && invStats.getName() != null ? invStats.getName() : "")
                .productCode(invStats != null && invStats.getProductCode() != null ? invStats.getProductCode() : "")
                .capacity(invStats != null && invStats.getCapacity() != null ? invStats.getCapacity() : "")
                .imageUrl(invStats != null ? invStats.getImageUrl() : null)
                .orderedCount(orderedCount)
                .issuedToPartnerCount(issuedToPartnerCount)
                .inCirculationCount(inCirculationCount)
                .withPartnerCount(withPartnerCount)
                .soldCount(soldCount)
                .damagedCount(damagedCount)
                .inStockCount(inStockCount)
                .returnedCount(returnedCount)
                .build();
    }
}