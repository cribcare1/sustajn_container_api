package com.auth.repository;

import com.auth.model.RestaurantFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RestaurantFeedbackRepository
        extends JpaRepository<RestaurantFeedback, Long> {

    List<RestaurantFeedback> findByRestaurantId(Long restaurantId);
}
