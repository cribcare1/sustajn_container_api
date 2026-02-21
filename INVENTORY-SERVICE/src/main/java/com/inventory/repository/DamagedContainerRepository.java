package com.inventory.repository;

import com.inventory.entity.DamagedContainer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DamagedContainerRepository extends JpaRepository<DamagedContainer, Long> {
    @Query("SELECT d FROM DamagedContainer d WHERE d.restaurantId = :restaurantId AND d.damagedByRestaurant = true")
    List<DamagedContainer> findByRestaurantId(Long restaurantId);

    @Query("SELECT d FROM DamagedContainer d WHERE d.damagedByUser = true")
    List<DamagedContainer> findAllIsDamageByCustomer();

    @Query("SELECT d FROM DamagedContainer d WHERE d.damagedByRestaurant = true")
    List<DamagedContainer> findAllIsDamageByRestaurant();
}
