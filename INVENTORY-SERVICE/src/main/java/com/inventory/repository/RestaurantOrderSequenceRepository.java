package com.inventory.repository;

import com.inventory.entity.RestaurantOrderSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RestaurantOrderSequenceRepository extends JpaRepository<RestaurantOrderSequence, Long> {

    Optional<RestaurantOrderSequence> findByRestaurantId(Long restaurantId);

}
