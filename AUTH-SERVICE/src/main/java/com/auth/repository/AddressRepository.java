package com.auth.repository;

import com.auth.model.Address;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findByUser_IdAndStatus(Long userId, String status);

    @Query("SELECT a FROM Address a WHERE a.user.id IN :userIds AND a.status = :status")
    List<Address> findByUserIdsAndStatusBulk(@Param("userIds") List<Long> userIds, @Param("status") String status);
}
