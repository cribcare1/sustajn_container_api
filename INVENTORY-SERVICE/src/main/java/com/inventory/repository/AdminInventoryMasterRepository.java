package com.inventory.repository;

import com.inventory.dto.InventoryWithContainerResponse;
import com.inventory.entity.AdminInventoryMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface AdminInventoryMasterRepository extends JpaRepository<AdminInventoryMaster,Long> {
    Optional<AdminInventoryMaster> findByContainerTypeId(Integer containerTypeId);



    @Query("""
        SELECT new com.inventory.dto.InventoryWithContainerResponse(
            m.id,
            c.id,
            c.name,
            c.description,
            c.capacityMl,
            c.material,
            c.colour,
            c.lengthCm,
            c.widthCm,
            c.heightCm,
            c.weightGrams,
            c.foodSafe,
            c.dishwasherSafe,
            c.microwaveSafe,
            c.maxTemperature,
            c.minTemperature,
            c.lifespanCycle,
            c.imageUrl,
            c.costPerUnit,
            m.totalContainers,
            m.availableContainers,
            c.productId
        )
        FROM AdminInventoryMaster m
        JOIN ContainerType c ON m.containerTypeId = c.id
        WHERE m.status = 'active' AND c.status = 'active'
        ORDER BY c.name ASC
    """)
    List<InventoryWithContainerResponse> getActiveInventoryWithContainerDetails();

    List<AdminInventoryMaster> findByContainerTypeIdIn(Set<Integer> typeIds);
}
