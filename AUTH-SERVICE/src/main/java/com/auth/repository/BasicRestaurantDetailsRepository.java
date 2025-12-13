package com.auth.repository;

import com.auth.model.BasicRestaurantDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BasicRestaurantDetailsRepository extends JpaRepository<BasicRestaurantDetails,Long> {
}
