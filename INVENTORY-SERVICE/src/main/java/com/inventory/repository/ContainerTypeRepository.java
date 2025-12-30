package com.inventory.repository;

import com.inventory.dto.ContainerTypeResponse;
import com.inventory.dto.ProductResponse;
import com.inventory.entity.ContainerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

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

    @Query("""
        SELECT new com.inventory.dto.ProductResponse(
            c.id,
            c.name,
            c.description,
            CAST(c.costPerUnit AS double),
            c.imageUrl,
            c.capacityMl,
            c.productId
        )
        FROM ContainerType c
        WHERE c.id IN :ids
        AND c.status = 'active'
    """)
    List<ProductResponse> findProductResponsesByIds(@Param("ids") List<Integer> ids);

    Optional<ContainerType> findByNameIgnoreCase(@NotBlank(message = "Container name is required") @Size(max = 100, message = "Container name must be less than 100 characters") String containerName);
}
