package com.inventory.repository;

import com.inventory.entity.DamagedContainerImages;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DamagedContainerImagesRepository extends JpaRepository<DamagedContainerImages, Long> {
}
