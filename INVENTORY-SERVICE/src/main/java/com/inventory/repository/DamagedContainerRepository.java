package com.inventory.repository;

import com.inventory.entity.DamagedContainer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DamagedContainerRepository extends JpaRepository<DamagedContainer, Long> {
}
