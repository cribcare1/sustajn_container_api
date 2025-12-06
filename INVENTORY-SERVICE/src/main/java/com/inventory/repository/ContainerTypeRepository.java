package com.inventory.repository;

import com.inventory.dto.ContainerTypeResponse;
import com.inventory.entity.ContainerType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ContainerTypeRepository extends JpaRepository<ContainerType,Integer> {
    boolean existsByNameIgnoreCase(String name);

    @Query("""
        SELECT new com.inventory.dto.ContainerTypeResponse(
            c.id, c.name, c.description, c.capacityMl,
            c.material, c.colour, c.lengthCm, c.widthCm,
            c.heightCm, c.weightGrams, c.maxTemperature,
            c.minTemperature, c.lifespanCycle, c.imageUrl, c.costPerUnit
        )
        FROM ContainerType c
        WHERE c.status = 'active'
    """)
    List<ContainerTypeResponse> findActiveContainerTypes();
}
