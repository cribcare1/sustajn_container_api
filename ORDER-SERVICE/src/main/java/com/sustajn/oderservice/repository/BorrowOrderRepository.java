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


}
