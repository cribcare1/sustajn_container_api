package com.sustajn.oderservice.repository;

import com.sustajn.oderservice.entity.ReturnOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ReturnOrderRepository extends JpaRepository<ReturnOrder,Long> {
    @Query(value = """
    SELECT r.*
    FROM return_orders r
    WHERE r.user_id = :userId
      AND EXTRACT(YEAR FROM r.returned_at) = :year
""", nativeQuery = true)
    List<ReturnOrder> findAllByUserIdAndYear(
            @Param("userId") Long userId,
            @Param("year") int year
    );

    List<ReturnOrder> findByRestaurantId(Long restaurantId);

}
