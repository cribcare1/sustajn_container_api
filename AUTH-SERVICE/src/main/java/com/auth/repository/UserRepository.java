package com.auth.repository;

import com.auth.enumDetails.AccountStatus;
import com.auth.enumDetails.UserType;
import com.auth.model.User;
import com.auth.response.RestaurantRegisterResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findByEmail(String userName);
    Optional<User> findByUserName(String userName);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    Page<User> findByUserTypeAndAccountStatus(UserType userType, AccountStatus status, Pageable pageable);

    @Query("""
    SELECT new com.auth.response.RestaurantRegisterResponse(
        u.id,
        u.fullName,
        u.email,
        u.phoneNumber,
        u.profilePictureUrl
    )
    FROM User u
    WHERE u.id IN :ids
      AND u.userType = :userType
      AND u.accountStatus = :accountStatus
""")
    List<RestaurantRegisterResponse> findRestaurantsByIds(
            @Param("ids") List<Long> ids,
            @Param("userType") UserType userType,
            @Param("accountStatus") AccountStatus accountStatus
    );

    @Query("SELECT r FROM User r " +
            "WHERE r.userType = 'RESTAURANT' " +
            "AND (" +
            "LOWER(REPLACE(r.fullName, ' ', '')) LIKE LOWER(CONCAT('%', REPLACE(:keyword, ' ', ''), '%')) " +
            "OR LOWER(REPLACE(r.address, ' ', '')) LIKE LOWER(CONCAT('%', REPLACE(:keyword, ' ', ''), '%'))" +
            ")" +
            "AND r.accountStatus = 'active'")
    List<User> searchRestaurantsByKeyword(@Param("keyword") String keyword);

}
