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

    @Query("SELECT u.customerId FROM User u WHERE u.customerId = :baseId")
    List<String> findCustomerIdStartingWith(String baseId);


    @Query("""
SELECT 
    u.id,
    u.fullName,
    u.phoneNumber,
    u.customerId,
    u.email,
    u.profilePictureUrl,
    u.subscriptionPlanId,

    b1.id, b1.bankName, b1.accountNumber, b1.iBanNumber, b1.taxNumber,

    c1.id, c1.cardHolderName, c1.cardNumber, c1.expiryDate,

    p1.id, p1.paymentGatewayId, p1.paymentGatewayName,

    a.id, a.addressType, a.flatDoorHouseDetails, 
    a.areaStreetCityBlockDetails, a.poBoxOrPostalCode,
    u.secondaryNumber
FROM User u

LEFT JOIN BankDetails b1 
    ON b1.userId = u.id AND b1.bankName IS NOT NULL AND b1.status = 'active'

LEFT JOIN BankDetails c1 
    ON c1.userId = u.id AND c1.cardNumber IS NOT NULL AND c1.status = 'active'

LEFT JOIN BankDetails p1 
    ON p1.userId = u.id AND p1.paymentGatewayId IS NOT NULL AND p1.status = 'active'

LEFT JOIN Address a ON a.userId = u.id

WHERE u.id = :userId
""")
    List<Object[]> getCustomerProfileDetailsByUserId(@Param("userId") Long userId);


    Optional<User> findByPhoneNumber(String phoneNumber);
}
