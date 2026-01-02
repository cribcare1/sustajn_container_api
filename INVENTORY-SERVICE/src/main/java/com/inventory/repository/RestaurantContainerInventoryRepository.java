package com.inventory.repository;

import com.inventory.dto.RestaurantContainerDetails;
import com.inventory.entity.RestaurantContainerInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface RestaurantContainerInventoryRepository extends JpaRepository<RestaurantContainerInventory,Long> {
    List<RestaurantContainerInventory> findAllByRestaurantIdAndContainerTypeIdIn(Long restaurantId, Set<Integer> containerTypeIds);

    @Query("""
    SELECT new com.inventory.dto.RestaurantContainerDetails(
        ct.id,
        ct.name,
        ct.description,
        ct.capacityMl,
        ct.imageUrl,
        ct.productId,
        inv.currentQuantity
    )
    FROM RestaurantContainerInventory inv
    JOIN ContainerType ct ON ct.id = inv.containerTypeId
    WHERE inv.restaurantId = :restaurantId
""")
    List<RestaurantContainerDetails> findContainersWithDetails(@Param("restaurantId") Long restaurantId);

}
