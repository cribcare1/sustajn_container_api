package com.auth.repository;

import com.auth.enumDetails.AccountStatus;
import com.auth.enumDetails.UserType;
import com.auth.model.User;
import com.auth.response.RestaurantRegisterResponse;
import com.auth.response.RestaurantSearchResponse;
import com.auth.response.UserSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
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

    @Query("""
            SELECT new com.auth.response.RestaurantSearchResponse(
                r.id,
                r.fullName,
                r.latitude,
                r.longitude,
                0.0,
                r.profilePictureUrl,
                a.id,
                a.addressType,
                a.flatDoorHouseDetails,
                a.areaStreetCityBlockDetails,
                a.poBoxOrPostalCode
            )
            FROM User r
            LEFT JOIN r.addresses a
            WHERE r.userType = 'RESTAURANT'
            AND LOWER(REPLACE(r.fullName, ' ', '')) LIKE LOWER(CONCAT('%', REPLACE(:keyword, ' ', ''), '%'))
            AND r.accountStatus = 'active'
            """)
    List<RestaurantSearchResponse> searchRestaurants(@Param("keyword") String keyword);


    @Query("SELECT u.customerId FROM User u WHERE u.customerId = :baseId")
    List<String> findCustomerIdStartingWith(String baseId);


    @Query(value = """
                SELECT 
                    u.user_id,
                    u.name,
                    u.phone_number,
                    u.customer_id,
                    u.email,
                    u.profile_picture_url,
                    u.subscription_plan_id,
            
                    b.id,
                    b.bank_name,
                    b.account_holder_name,
                    b.i_ban_number,
                    b.bic_number,
            
                    c.id,
                    c.card_holder_name,
                    c.card_number,
                    c.expiry_date,
            
                    p.id,
                    p.payment_gateway_id,
                    p.payment_gateway_name,
            
                    u.secondary_number,
                    u.date_of_birth,
            
                    ca.id,
                    ca.contact_person_name,
                    ca.contact_email,
                    ca.tread_license_number,
                    ca.vat_number,
                    ca.contact_number,
                    ca.registration_number,
            
                    COALESCE(
                        json_agg(
                            DISTINCT jsonb_build_object(
                                'id', a.id,
                                'addressType', a.address_type,
                                'flatDoorHouseDetails', a.flat_door_house_details,
                                'areaStreetCityBlockDetails', a.area_street_city_block_details,
                                'poBoxOrPostalCode', a.po_box_or_postal_code
                            )
                        ) FILTER (WHERE a.id IS NOT NULL),
                        '[]'
                    ) AS addresses,
            
                    COALESCE(
                        json_agg(
                            DISTINCT jsonb_build_object(
                                'id', social.id,
                                'socialMediaType', social.social_media_type,
                                'link', social.link
                            )
                        ) FILTER (WHERE social.id IS NOT NULL),
                        '[]'
                    ) AS socialMediaDetails,
            
                    business.id,
                    business.business_type,
                    business.website_details
            
                FROM users u
            
                LEFT JOIN bank_details b 
                    ON b.user_id = u.user_id 
                   AND b.bank_name IS NOT NULL 
                   AND b.status = 'active'
            
                LEFT JOIN bank_details c 
                    ON c.user_id = u.user_id 
                   AND c.card_number IS NOT NULL 
                   AND c.status = 'active'
            
                LEFT JOIN bank_details p 
                    ON p.user_id = u.user_id 
                   AND p.payment_gateway_id IS NOT NULL 
                   AND p.status = 'active'
            
                LEFT JOIN contact_registration_details ca 
                    ON ca.user_id = u.user_id
            
                LEFT JOIN address a 
                    ON a.user_id = u.user_id 
                   AND a.status = 'active'
            
                LEFT JOIN social_media_details social
                    ON social.restaurant_id = u.user_id
            
                LEFT JOIN  basic_restaurant_details business
                    ON business.restaurant_id = u.user_id
            
                WHERE u.user_id = :userId
            
                GROUP BY 
                    u.user_id,
                    b.id,
                    c.id,
                    p.id,
                    ca.id,
                    business.id
            """, nativeQuery = true)
    List<Object[]> getCustomerProfileDetailsByUserId(@Param("userId") Long userId);


    Optional<User> findByPhoneNumber(String phoneNumber);

    @Query("SELECT u.id, u.fullName, u.profilePictureUrl, a.areaStreetCityBlockDetails, a.poBoxOrPostalCode  " +
            "FROM User u " +
            "JOIN Address a on a.user.id = u.id " +
            "WHERE u.userType = :userType " +
            "AND u.accountStatus = :accountStatus")
    List<Object[]> findAllActiveRestaurants(UserType userType, AccountStatus accountStatus);

    @Query("""
                SELECT 
                    u.id,
                    u.fullName,
                    u.phoneNumber,
                    u.secondaryNumber,
                    u.subscriptionPlanId,
                    null,
            
                    brd.id,
                    brd.businessType,
                    brd.websiteDetails,
                    brd.cuisine,
            
                    crd.id,
                    crd.contactPersonName,
                    crd.contactEmail,
                    crd.treadLicenseNumber,
                    crd.vatNumber,
                    crd.contactNumber,
                    crd.registrationNumber
                FROM User u
                LEFT JOIN BasicRestaurantDetails brd ON brd.restaurantId = u.id
                LEFT JOIN ContactRegistrationDetails crd ON crd.userId = u.id
                WHERE u.id = :restaurantId
            """)
    List<Object[]> findRestaurantDetailsById(Long restaurantId);

    @Query("SELECT u FROM User u WHERE u.customerId = :customerId")
    Optional<User> findUserByCustomerId(String customerId);

    // Fetch Customer IDs for a bulk list of numeric User IDs
    @Query("SELECT u.id, u.customerId FROM User u WHERE u.id IN :userIds")
    List<Object[]> findCustomerIdsByUserIds(@Param("userIds") List<Long> userIds);

    // Fetch Restaurant Name and Address by their IDs
//    @Query("SELECT u.id, u.fullName, a FROM User u " +
//            "  LEFT JOIN   Address a " +
//            "WHERE u.id IN :userIds")
//    List<Object[]> findPartnerDetailsByIds(@Param("userIds") List<Long> userIds);

    @Query("""
    SELECT u.id, u.fullName, a
    FROM User u
    LEFT JOIN u.addresses a
    WHERE u.id IN :userIds
""")
    List<Object[]> findPartnerDetailsByIds(@Param("userIds") List<Long> userIds);

    @Query("SELECT new com.auth.response.UserSummaryDto(u.id, u.customerId, u.fullName) " +
            "FROM User u WHERE u.id IN :userIds")
    List<UserSummaryDto> findUserSummariesByUserIds(@Param("userIds") List<Long> userIds);
}
