package com.auth.repository;

import com.auth.model.Address;
import org.apache.catalina.mapper.Mapper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;;

import java.util.List;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
    void deleteByUserId(Long id);

    List<Address> findByUserIdAndStatusOrderByCreatedAtDesc(Long id, String active);

    List<Address> findByUserIdAndStatus(Long userId, String status);
}
