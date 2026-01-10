package com.inventory.repository;

import com.inventory.entity.AdminOrder;
import com.inventory.response.RestaurantOrderedResponse;
import feign.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AdminOrderRepository extends JpaRepository<AdminOrder,Long> {

    @Query("""
        SELECT new com.inventory.response.RestaurantOrderedResponse(
            ct.name,
            ao.orderId,
            ao.orderDate,
            ao.type,
            ao.status,
            ao.restaurantRemark,
            ao.adminRemark,
            ao.decisionAt,
            aoi.requestedQty,
            aoi.approvedQty
        )
        FROM AdminOrder ao
        JOIN ao.items aoi
        LEFT JOIN ContainerType ct ON ct.id = aoi.containerTypeId
        WHERE ao.restaurantId = :restaurantId
        ORDER BY ao.orderDate DESC
    """)
    List<RestaurantOrderedResponse> findOrdersByRestaurantId(@Param("restaurantId") Long restaurantId);
}
