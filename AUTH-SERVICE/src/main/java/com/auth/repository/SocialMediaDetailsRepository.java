package com.auth.repository;

import com.auth.model.SocialMediaDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SocialMediaDetailsRepository extends JpaRepository<SocialMediaDetails,Long> {
}
