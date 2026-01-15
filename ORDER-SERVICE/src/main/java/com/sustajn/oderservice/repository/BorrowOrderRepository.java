package com.sustajn.oderservice.repository;

import com.sustajn.oderservice.entity.BorrowOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BorrowOrderRepository extends JpaRepository<BorrowOrder,Long> {

    @Query("""
    SELECT b
    FROM BorrowOrder b
    WHERE b.userId = :userId
      AND b.productId IN :productIds
      AND b.quantity > b.returnedQuantity
    ORDER BY b.productId, b.borrowedAt ASC
""")
    List<BorrowOrder> findAllPendingBorrowsFIFO(
        @Param("userId") Long userId,
        @Param("productIds") List<Long> productIds
);


    @Query("""
    SELECT b
    FROM BorrowOrder b
    JOIN Order o ON b.orderId = o.id
    WHERE o.orderStatus = 'APPROVED'
      AND b.userId = :userId
    """)
    List<BorrowOrder> getAllTheApprovedBorrowOrdersByUserId(Long userId);

//    @Query("""
//        SELECT b
//        FROM BorrowOrder b
//        JOIN Order o ON b.orderId = o.id
//        WHERE b.userId = :userId
//          AND FUNCTION('YEAR', o.orderDate) = :year
//          AND o.orderStatus = 'APPROVED'
//        """)
//    List<BorrowOrder> findAllByUserIdAndYear(
//            @Param("userId") Long userId,
//            @Param("year") int year
//    );

    @Query(value = """
    SELECT b.*
    FROM borrow_orders b
    JOIN orders o ON b.order_id = o.id
    WHERE b.user_id = :userId
      AND EXTRACT(YEAR FROM o.order_date) = :year
      AND o.order_status = 'APPROVED'
""", nativeQuery = true)
    List<BorrowOrder> findAllByUserIdAndYear(
            @Param("userId") Long userId,
            @Param("year") int year
    );

    List<BorrowOrder> findAllByOrderId(Long orderId);

    @Query(value = """
SELECT 
    b.order_id,
    b.product_id,
    b.quantity AS borrowedQty,
    COALESCE(SUM(r.returned_quantity), 0) AS returnedQty,
    (b.quantity - COALESCE(SUM(r.returned_quantity), 0)) AS remainingQty,
    o.order_date
FROM borrow_orders b
JOIN orders o ON b.order_id = o.id
LEFT JOIN return_orders r ON r.borrow_order_id = b.id
WHERE b.user_id = :userId
GROUP BY b.order_id, b.product_id, b.quantity, o.order_date
""", nativeQuery = true)
    List<Object[]> getProductBorrowReturnSummary(@Param("userId") Long userId);


    List<BorrowOrder> findByRestaurantId(Long restaurantId);

    @Query("""
        SELECT COALESCE(SUM(b.quantity), 0)
        FROM BorrowOrder b
        WHERE b.restaurantId = :restaurantId
          AND b.productId = :productId
    """)
    Integer getTotalLeasedContainers(@Param("restaurantId") Long restaurantId,
                                     @Param("productId") Integer productId);
}
