package com.sustajn.oderservice.repository;

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
}
