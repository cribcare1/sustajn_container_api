package com.sustajn.oderservice.repository;

import com.sustajn.oderservice.dto.CustomerSoldHistoryRawDto;
import com.sustajn.oderservice.dto.SoldHistoryRawData;
import com.sustajn.oderservice.entity.SoldOrder;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SoldOrderRepository extends JpaRepository<SoldOrder, Integer> {

    // Add this inside your existing SoldOrderRepository interface

    @Query("SELECT new com.sustajn.oderservice.dto.SoldHistoryRawData(" +
            "s.productId, s.soldQuantity, s.unitPrice, " +
            "b.borrowedAt, b.dueDate, s.soldAt) " +
            "FROM SoldOrder s " +
            "JOIN BorrowOrder b ON s.orderId = b.id " +
            "WHERE s.restaurantId = :restaurantId " +
            "ORDER BY s.soldAt DESC")
    List<SoldHistoryRawData> getRealSoldHistory(@Param("restaurantId") Long restaurantId);

    @Query(value = "SELECT " +
            "  s.product_id AS productId, " +
            "  s.sold_quantity AS soldQuantity, " +
            "  s.unit_price AS unitPrice, " +
            "  s.total_amount AS totalAmount, " +
            "  b.borrowed_at AS borrowedAt, " +
            "  COALESCE(b.effective_due_date, b.due_date) AS dueDate, " +
            "  s.sold_at AS soldAt " +
            "FROM sold_orders s " +
            "LEFT JOIN borrow_orders b ON s.order_id = b.order_id AND s.product_id = b.product_id " +
            "WHERE s.user_id = :userId " +
            "ORDER BY s.sold_at DESC", nativeQuery = true)
    List<Object[]> findCustomerSoldHistoryRawDataNative(@Param("userId") Long userId);
    @Query("""
        SELECT s FROM SoldOrder s 
        WHERE (:productId IS NULL OR s.productId = :productId) 
          AND s.userId IS NOT NULL
        ORDER BY s.soldAt DESC
    """)
    List<SoldOrder> findUserSoldOrders(@Param("productId") Long productId);

    // Fetch sold orders for RESTAURANT / PARTNER tab (where restaurantId IS NOT NULL)
    @Query("""
        SELECT s FROM SoldOrder s 
        WHERE (:productId IS NULL OR s.productId = :productId) 
          AND s.restaurantId IS NOT NULL
        ORDER BY s.soldAt DESC
    """)
    List<SoldOrder> findRestaurantSoldOrders(@Param("productId") Long productId);
}
