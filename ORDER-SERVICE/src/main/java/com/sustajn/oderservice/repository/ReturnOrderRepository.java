package com.sustajn.oderservice.repository;

import com.sustajn.oderservice.dto.LeasedReturnedResponse;
import com.sustajn.oderservice.entity.ReturnOrder;
import com.sustajn.oderservice.projection.LeasedReturnedCountWithTimeGraphProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ReturnOrderRepository extends JpaRepository<ReturnOrder,Long> {
    @Query(value = """
    SELECT r.*
    FROM return_orders r
    WHERE r.user_id = :userId
      AND r.returned_at BETWEEN :fromDate AND :toDate
""", nativeQuery = true)
    List<ReturnOrder> findAllByUserIdBetweenDates(
            @Param("userId") Long userId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    List<ReturnOrder> findByRestaurantId(Long restaurantId);


    @Query("""
    SELECT new com.sustajn.oderservice.dto.LeasedReturnedResponse(
        CONCAT(
            TRIM(FUNCTION('TO_CHAR', DATE(r.returnedAt), 'Month')),
            '-',
            FUNCTION('TO_CHAR', DATE(r.returnedAt), 'YYYY')
        ),
        CAST(FUNCTION('TO_CHAR', DATE(r.returnedAt), 'DD.MM.YYYY') AS string),
        SUM(r.returnedQuantity)
    )
    FROM ReturnOrder r
    WHERE r.restaurantId = :restaurantId
      AND r.productId = :productId
    GROUP BY DATE(r.returnedAt)
    ORDER BY DATE(r.returnedAt)
""")
    List<LeasedReturnedResponse> getReturnedMonthYearDetails(
            @Param("restaurantId") Long restaurantId,
            @Param("productId") Integer productId
    );

    @Query(
            value = """
            SELECT
                gs.hour || '-' || (gs.hour + 1) AS time,
                COALESCE(SUM(r.returned_quantity)::int, 0) AS leasedReturnedCount
            FROM generate_series(0,23) AS gs(hour)
            LEFT JOIN return_orders r
                ON EXTRACT(HOUR FROM r.returned_at) = gs.hour
               AND r.restaurant_id = :restaurantId
               AND r.product_id = :productId
               AND r.returned_at BETWEEN :startTime AND :endTime
            GROUP BY gs.hour
            ORDER BY gs.hour
            """,
            nativeQuery = true
    )
    List<LeasedReturnedCountWithTimeGraphProjection> getReturnedCountWithTimeGraph(
            @Param("restaurantId") Long restaurantId,
            @Param("productId") Integer productId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    @Query("""
        SELECT COALESCE(SUM(r.returnedQuantity), 0) 
        FROM ReturnOrder r 
        WHERE r.restaurantId = :restaurantId 
          AND (:productId IS NULL OR r.productId = :productId) 
          AND (cast(:startDate as timestamp) IS NULL OR r.returnedAt >= :startDate) 
          AND (cast(:endDate as timestamp) IS NULL OR r.returnedAt <= :endDate)
    """)
    Integer getTotalReturned(
            @Param("restaurantId") Long restaurantId,
            @Param("productId") Long productId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

}
