package com.inventory.repository;

import com.inventory.Constant.TransactionType;
import com.inventory.entity.AdminOrder;
import com.inventory.entity.AdminOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AdminOrderItemRepository extends JpaRepository<AdminOrderItem,Long> {
    List<AdminOrderItem> findAllByOrder(AdminOrder order);

    @Query("""
        SELECT 
            ct.id,
            ct.name,
            ct.productId,
            ct.capacityMl,
            SUM(oi.approvedQty)
        FROM AdminOrderItem oi
        JOIN oi.order o
        JOIN ContainerType ct ON ct.id = oi.containerTypeId
        WHERE o.restaurantId = :restaurantId
          AND o.status = com.inventory.Constant.AdminOrderStatus.APPROVED
        GROUP BY ct.id, ct.name, ct.productId, ct.capacityMl
    """)
    List<Object[]> findIssuedProducts(@Param("restaurantId") Long restaurantId);

    @Query(value = """
    SELECT 
        DATE(o.order_date) as order_date,
        SUM(oi.approved_qty)
    FROM admin_order_items oi
    JOIN admin_orders o ON o.id = oi.admin_order_id
    WHERE o.restaurant_id = :restaurantId
      AND o.status = 'APPROVED'
      AND oi.container_type_id = :productId
    GROUP BY DATE(o.order_date)
    ORDER BY DATE(o.order_date) DESC
""", nativeQuery = true)
    List<Object[]> findDateWiseIssuedQty(
            @Param("restaurantId") Long restaurantId,
            @Param("productId") Integer productId
    );

    @Query("""
        SELECT 
            ct.id,
            ct.name,
            ct.productId,
            ct.capacityMl,
            SUM(oi.approvedQty) 
        FROM AdminOrderItem oi
        JOIN oi.order o
        JOIN ContainerType ct ON ct.id = oi.containerTypeId
        WHERE o.restaurantId = :restaurantId
          AND o.status = com.inventory.Constant.AdminOrderStatus.APPROVED 
          AND o.type = com.inventory.Constant.TransactionType.RETURN  
        GROUP BY ct.id, ct.name, ct.productId, ct.capacityMl
    """)
    List<Object[]> findReturnedProducts(@Param("restaurantId") Long restaurantId);

    // 2. Get Date-wise History for a Specific Returned Product
    @Query(value = """
    SELECT 
        DATE(o.order_date) as order_date, 
        SUM(oi.approved_qty)
    FROM admin_order_items oi
    JOIN admin_orders o ON o.id = oi.admin_order_id
    WHERE o.restaurant_id = :restaurantId 
      AND o.status = 'APPROVED'
      AND o.type = 'RETURN'       
      AND oi.container_type_id = :productId
    GROUP BY DATE(o.order_date)
    ORDER BY DATE(o.order_date) DESC""", nativeQuery = true)
    List<Object[]> findDateWiseReturnedQty(
            @Param("restaurantId") Long restaurantId,
            @Param("productId") Integer productId
    );

    @Query("SELECT COALESCE(SUM(i.approvedQty), 0) " +
            "FROM AdminOrderItem i JOIN i.order o " +
            "WHERE i.containerTypeId = :containerTypeId " +
            "AND o.type = :type")
    Integer sumQtyByContainerTypeIdAndType(
            @Param("containerTypeId") Integer containerTypeId,
            @Param("type") TransactionType type
    );

    @Query(value = """
        SELECT 
            o.id AS orderId,
            o.restaurant_id AS restaurantId,
            ct.product_id AS containerCode,
            oi.approved_qty AS quantity,
            o.order_date AS orderedDate,
            o.decision_at AS deliveredDate
        FROM admin_order_items oi
        JOIN admin_orders o ON o.id = oi.admin_order_id
        JOIN container_types ct ON ct.id = oi.container_type_id
        WHERE oi.container_type_id = :containerTypeId
          AND o.status = 'APPROVED'
        ORDER BY o.order_date DESC
        """, nativeQuery = true)
    List<Object[]> findIssuedDetailsByContainerType(@Param("containerTypeId") Integer containerTypeId);

    @Query("""
    SELECT COALESCE(SUM(oi.approvedQty), 0)
    FROM AdminOrderItem oi
    JOIN oi.order o
    WHERE oi.containerTypeId = :containerTypeId
      AND o.status = com.inventory.Constant.AdminOrderStatus.APPROVED
      AND o.type = com.inventory.Constant.TransactionType.BORROW
""")
    Integer sumIssuedToPartnerCountByContainerTypeId(@Param("containerTypeId") Integer containerTypeId);
}
