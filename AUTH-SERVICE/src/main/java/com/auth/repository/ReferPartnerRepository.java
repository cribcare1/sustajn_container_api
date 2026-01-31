package com.auth.repository;

import com.auth.model.ReferPartner;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReferPartnerRepository extends JpaRepository<ReferPartner,Long> {

    @Query("SELECT rp FROM ReferPartner rp WHERE rp.contactEmail = :partnerEmail AND rp.contactPhone = :partnerPhone")
    Optional<ReferPartner> findByEmailAndPhone(String partnerEmail, String partnerPhone);
}
