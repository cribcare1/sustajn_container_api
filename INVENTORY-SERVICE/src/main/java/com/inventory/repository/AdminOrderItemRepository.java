package com.inventory.repository;

import com.inventory.entity.AdminOrder;
import com.inventory.entity.AdminOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminOrderItemRepository extends JpaRepository<AdminOrderItem,Long> {
    List<AdminOrderItem> findAllByOrder(AdminOrder order);
}
