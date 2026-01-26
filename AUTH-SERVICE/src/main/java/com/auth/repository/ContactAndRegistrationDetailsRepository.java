package com.auth.repository;

import com.auth.model.ContactRegistrationDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactAndRegistrationDetailsRepository extends JpaRepository<ContactRegistrationDetails,Long> {
}
