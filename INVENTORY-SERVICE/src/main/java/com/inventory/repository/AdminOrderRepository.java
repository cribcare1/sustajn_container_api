package com.inventory.repository;

import com.inventory.Constant.AdminOrderStatus;
import com.inventory.Constant.TransactionType;
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

    @Query("SELECT oi.containerTypeId, SUM(COALESCE(oi.approvedQty, oi.requestedQty)) " +
            "FROM AdminOrder o JOIN o.items oi " +
            "WHERE o.type = :type AND o.status = :status " +
            "GROUP BY oi.containerTypeId")
    List<Object[]> getProcessedQuantitiesGroupedByType(
            @Param("type") TransactionType type,
            @Param("status") AdminOrderStatus status
    );

    @Query("SELECT oi.containerTypeId, SUM(COALESCE(oi.approvedQty, oi.requestedQty)) " +
            "FROM AdminOrder o JOIN o.items oi " +
            "WHERE o.restaurantId = :restaurantId AND o.type = :type AND o.status = :status " +
            "GROUP BY oi.containerTypeId")
    List<Object[]> getRestaurantAdminBorrows(
            @Param("restaurantId") Long restaurantId,
            @Param("type") TransactionType type,
            @Param("status") AdminOrderStatus status
    );

    List<AdminOrder> findByStatusOrderByOrderDateDesc(AdminOrderStatus status);
}
