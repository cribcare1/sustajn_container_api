package com.inventory.repository;

import com.inventory.entity.AdminOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminOrderRepository extends JpaRepository<AdminOrder,Long> {
}
