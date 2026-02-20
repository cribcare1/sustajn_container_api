package com.inventory.repository;

import com.inventory.dto.SoldContainersDateWiseResponse;
import com.inventory.entity.SoldContainers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SoldContainerRepository extends JpaRepository<SoldContainers, Integer> {

    List<SoldContainers> findByRestaurantId(Long restaurantId);
}
