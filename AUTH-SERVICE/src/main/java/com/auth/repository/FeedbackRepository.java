package com.auth.repository;

import com.auth.model.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    List<Feedback> findByRestaurantId(Long restaurantId);

    // RENAMED from findBySenderId
    List<Feedback> findByCustomerId(Long customerId);
}