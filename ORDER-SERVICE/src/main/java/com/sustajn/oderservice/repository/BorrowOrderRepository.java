package com.sustajn.oderservice.repository;

import com.sustajn.oderservice.dto.BorrowOrderResponse;
import com.sustajn.oderservice.dto.LeasedReturnedResponse;
import com.sustajn.oderservice.entity.BorrowOrder;
import com.sustajn.oderservice.projection.LeasedReturnedCountWithTimeGraphProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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

//    @Query(value = """
//    SELECT b.*
//    FROM borrow_orders b
//    JOIN orders o ON b.order_id = o.id
//    WHERE b.user_id = :userId
//      AND EXTRACT(YEAR FROM o.order_date) = :year
//      AND o.order_status = 'APPROVED'
//""", nativeQuery = true)
//    List<BorrowOrder> findAllByUserIdAndYear(
//            @Param("userId") Long userId,
//            @Param("year") int year
//    );

    @Query(value = """
    SELECT b.*
    FROM borrow_orders b
    JOIN orders o ON b.order_id = o.id
    WHERE b.user_id = :userId
      AND b.borrowed_at BETWEEN :fromDate AND :toDate
      AND o.order_status = 'APPROVED'
""", nativeQuery = true)
    List<BorrowOrder> findAllByUserIdBetweenDates(
            @Param("userId") Long userId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    List<BorrowOrder> findAllByOrderId(Long orderId);

    @Query(value = """
            SELECT
                b.order_id,
                b.product_id,
                b.quantity AS borrowedQty,
                COALESCE(b.returned_quantity, 0) AS returnedQty,
                (b.quantity - COALESCE(b.returned_quantity, 0)) AS remainingQty,
                b.due_date,
                o.order_date
            FROM borrow_orders b
            JOIN orders o
                ON o.id = b.order_id
            WHERE b.user_id = :userId
            AND (b.quantity - COALESCE(b.returned_quantity, 0)) > 0
""", nativeQuery = true)
    List<BorrowOrderResponse> getProductBorrowReturnSummary(@Param("userId") Long userId);

    List<BorrowOrder> findByRestaurantId(Long restaurantId);

    @Query("""
    SELECT 
        COALESCE(SUM(b.quantity), 0),
        COALESCE(SUM(b.returnedQuantity), 0)
    FROM BorrowOrder b
    WHERE b.restaurantId = :restaurantId
      AND b.productId = :productId
""")
    List<Object[]> getLeasedAndReturnedCounts(@Param("restaurantId") Long restaurantId,
                                              @Param("productId") Integer productId);



    @Query("""
    SELECT new com.sustajn.oderservice.dto.LeasedReturnedResponse(
        CONCAT(
            TRIM(FUNCTION('TO_CHAR', DATE(b.borrowedAt), 'Month')),
            '-',
            FUNCTION('TO_CHAR', DATE(b.borrowedAt), 'YYYY')
        ),
        CAST(FUNCTION('TO_CHAR', DATE(b.borrowedAt), 'DD.MM.YYYY') AS string),
        SUM(b.quantity)
    )
    FROM BorrowOrder b
    WHERE b.restaurantId = :restaurantId
      AND b.productId = :productId
    GROUP BY DATE(b.borrowedAt)
    ORDER BY DATE(b.borrowedAt)
""")
    List<LeasedReturnedResponse> getLeasedMonthYearDetails(
            @Param("restaurantId") Long restaurantId,
            @Param("productId") Integer productId
    );


    @Query(
            value = """
            SELECT
                gs.hour || '-' || (gs.hour + 1) AS time,
                COALESCE(SUM(b.quantity)::int, 0) AS leasedReturnedCount
            FROM generate_series(0,23) AS gs(hour)
            LEFT JOIN borrow_orders b
                ON EXTRACT(HOUR FROM b.borrowed_at) = gs.hour
               AND b.restaurant_id = :restaurantId
               AND b.product_id = :productId
               AND b.borrowed_at BETWEEN :startTime AND :endTime
            GROUP BY gs.hour
            ORDER BY gs.hour
            """,
            nativeQuery = true
    )
    List<LeasedReturnedCountWithTimeGraphProjection> getLeasedCountWithTimeGraph(
            @Param("restaurantId") Long restaurantId,
            @Param("productId") Integer productId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // Only active (not fully returned)
    @Query("""
        SELECT b FROM BorrowOrder b
        WHERE b.quantity > b.returnedQuantity
        """)
    List<BorrowOrder> findActiveBorrowOrders();

    List<BorrowOrder> findByOrderId(Long orderId);

    @Query("""
    SELECT b.productId, SUM(b.quantity - b.returnedQuantity)
    FROM BorrowOrder b
    WHERE b.restaurantId = :restaurantId
      AND (b.quantity - b.returnedQuantity) > 0
    GROUP BY b.productId
""")
    List<Object[]> findUsageByRestaurant(Long restaurantId);

    @Query("""
        SELECT COALESCE(SUM(b.quantity), 0) 
        FROM BorrowOrder b 
        WHERE b.restaurantId = :restaurantId 
          AND (:productId IS NULL OR b.productId = :productId) 
          AND (cast(:startDate as timestamp) IS NULL OR b.borrowedAt >= :startDate) 
          AND (cast(:endDate as timestamp) IS NULL OR b.borrowedAt <= :endDate)
    """)
    Integer getTotalLeased(
            @Param("restaurantId") Long restaurantId,
            @Param("productId") Long productId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("""
    SELECT b FROM BorrowOrder b
    WHERE 
        (b.effectiveDueDate IS NOT NULL AND b.effectiveDueDate < :currentTime
            OR b.effectiveDueDate IS NULL AND b.dueDate < :currentTime)
        AND b.returnedQuantity < b.quantity
        AND (b.isSold = false OR b.isSold IS NULL)
""")
    List<BorrowOrder> findOverdueOrders(@Param("currentTime") LocalDateTime currentTime);

    @Query("SELECT COALESCE(SUM(b.quantity - b.returnedQuantity), 0) FROM BorrowOrder b WHERE b.productId = :productId")
    Integer getInCirculationCount(@Param("productId") Long productId);

    // Bulk fetch: Calculates circulation counts grouped by Product ID
    @Query("SELECT b.productId, COALESCE(SUM(b.quantity - b.returnedQuantity), 0) " +
            "FROM BorrowOrder b WHERE b.quantity > b.returnedQuantity GROUP BY b.productId")
    List<Object[]> getCirculationCountsForAllProducts();


    // Group active circulation counts by numeric User ID
    @Query("SELECT b.userId, COALESCE(SUM(b.quantity - b.returnedQuantity), 0) " +
            "FROM BorrowOrder b " +
            "WHERE b.productId = :productId AND b.quantity > b.returnedQuantity " +
            "GROUP BY b.userId")
    List<Object[]> getCirculationPerUserForProduct(@Param("productId") Long productId);

    Long findUserIdByOrderId(Long orderId);
}
