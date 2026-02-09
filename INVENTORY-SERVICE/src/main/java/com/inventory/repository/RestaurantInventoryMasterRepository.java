package com.inventory.repository;

import com.inventory.entity.RestaurantInventoryMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface RestaurantInventoryMasterRepository extends JpaRepository<RestaurantInventoryMaster, Long> {
    List<RestaurantInventoryMaster> findAllByRestaurantIdAndContainerTypeIdIn(Long restaurantId, Set<Integer> containerTypeIds);
}
