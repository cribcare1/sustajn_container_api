package com.inventory.repository;

import com.inventory.dto.RestaurantInventoryViewResponse;
import com.inventory.entity.AdminRestaurantInventoryDetails;
import com.inventory.response.RestaurantContainerInventoryResponse;
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


    @Query("""
    SELECT new com.inventory.response.RestaurantContainerInventoryResponse(
        r.id,
        r.restaurantId,
        r.containerTypeId,
        c.name,
        c.productId,
        c.imageUrl,
        c.capacityMl,
        r.currentQuantity
    )
    FROM RestaurantContainerInventory r
    JOIN ContainerType c 
        ON r.containerTypeId = c.id AND c.status = 'active'                                 
    WHERE r.restaurantId = :restaurantId
""")
    List<RestaurantContainerInventoryResponse> getRestaurantContainerInventoryByRestaurantId(Long restaurantId);

    @Query("SELECT COALESCE(SUM(a.containerCount), 0) FROM AdminRestaurantInventoryDetails a WHERE a.containerTypeId = :containerTypeId AND a.actionType = :actionType")
    Integer getTotalByActionType(@Param("containerTypeId") Integer containerTypeId, @Param("actionType") String actionType);

    @Query("SELECT COALESCE(SUM(d.containerCount), 0) FROM AdminRestaurantInventoryDetails d WHERE d.containerTypeId = :containerTypeId AND d.actionType = 'BORROW'")
    Integer sumIssuedToPartnerCount(@Param("containerTypeId") Integer containerTypeId);

}
