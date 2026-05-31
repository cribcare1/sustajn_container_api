package com.auth.repository;

import com.auth.model.SocialMediaDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SocialMediaDetailsRepository extends JpaRepository<SocialMediaDetails,Long> {
    List<SocialMediaDetails> findByRestaurantId(Long restaurantId);
}
