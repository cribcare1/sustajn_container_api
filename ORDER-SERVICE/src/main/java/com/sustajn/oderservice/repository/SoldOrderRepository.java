package com.sustajn.oderservice.repository;

import com.sustajn.oderservice.entity.SoldOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SoldOrderRepository extends JpaRepository<SoldOrder, Integer> {
}
