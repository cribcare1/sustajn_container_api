package com.auth.repository;

import com.auth.model.BankDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BankDetailsRepository extends JpaRepository<BankDetails, Long> {

    Optional<BankDetails> findByUserId(Long userId);

    void deleteByUserId(Long id);
}

