package com.inventory.repository;

import com.inventory.entity.DamagedContainer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DamagedContainerRepository extends JpaRepository<DamagedContainer, Long> {
    @Query("SELECT d FROM DamagedContainer d WHERE d.restaurantId = :restaurantId AND d.damagedByRestaurant = true")
    List<DamagedContainer> findByRestaurantId(Long restaurantId);

    @Query("SELECT d FROM DamagedContainer d WHERE d.damagedByUser = true")
    List<DamagedContainer> findAllIsDamageByCustomer();

    @Query("SELECT d FROM DamagedContainer d WHERE d.damagedByRestaurant = true")
    List<DamagedContainer> findAllIsDamageByRestaurant();

    @Query("""
        SELECT COUNT(d.id) 
        FROM DamagedContainer d 
        WHERE d.restaurantId = :restaurantId 
          AND (:containerTypeId IS NULL OR d.containerTypeId = :containerTypeId) 
          AND (cast(:startDate as timestamp) IS NULL OR d.createdAt >= :startDate) 
          AND (cast(:endDate as timestamp) IS NULL OR d.createdAt <= :endDate)
    """)
    Integer countDamagedContainers(
            @Param("restaurantId") Long restaurantId,
            @Param("containerTypeId") Integer containerTypeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
