package com.inventory.repository;

import com.inventory.entity.DamagedContainerImages;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Repository
public interface DamagedContainerImagesRepository extends JpaRepository<DamagedContainerImages, Long> {
    List<DamagedContainerImages> findByDamageIdIn(Set<Long> damageIds);
}
