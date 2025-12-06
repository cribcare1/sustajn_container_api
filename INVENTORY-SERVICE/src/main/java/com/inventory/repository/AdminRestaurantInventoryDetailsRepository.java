package com.inventory.repository;

import com.inventory.dto.RestaurantInventoryViewResponse;
import com.inventory.entity.AdminRestaurantInventoryDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AdminRestaurantInventoryDetailsRepository extends JpaRepository<AdminRestaurantInventoryDetails,Long> {

    @Query("""
    SELECT new com.inventory.dto.RestaurantInventoryViewResponse(
        d.containerTypeId,
        c.name,
        m.totalContainers,
        m.availableContainers,
        
        CASE WHEN d.actionType = 'BORROW' THEN d.containerCount ELSE 0 END,
        CASE WHEN d.actionType = 'RETURN' THEN d.containerCount ELSE 0 END,


        d.actionType
    )
    FROM AdminRestaurantInventoryDetails d
    JOIN AdminInventoryMaster m ON m.containerTypeId = d.containerTypeId
    JOIN ContainerType c ON c.id = d.containerTypeId
    WHERE d.restaurantId = :restaurantId
    ORDER BY d.createdOn DESC
""")
    List<RestaurantInventoryViewResponse> getRestaurantInventoryLogs(@Param("restaurantId") Long restaurantId);


}
