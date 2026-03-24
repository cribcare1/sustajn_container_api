package com.inventory.repository;

import com.inventory.entity.RestaurantInventoryMaster;
import com.inventory.validation.CreateGroup;
import feign.Param;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface RestaurantInventoryMasterRepository extends JpaRepository<RestaurantInventoryMaster, Long> {
    List<RestaurantInventoryMaster> findAllByRestaurantIdAndContainerTypeIdIn(Long restaurantId, Set<Integer> containerTypeIds);

    RestaurantInventoryMaster findByRestaurantIdAndContainerTypeId(Long restaurantId,  Integer containerTypeId);

    @Query("SELECT COALESCE(SUM(r.availableContainers), 0) FROM RestaurantInventoryMaster r WHERE r.containerTypeId = :containerTypeId")
    Integer getTotalWithPartner(@Param("containerTypeId") Integer containerTypeId);
}
