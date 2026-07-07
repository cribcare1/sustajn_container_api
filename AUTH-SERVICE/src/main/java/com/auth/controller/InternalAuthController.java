package com.auth.controller;

import com.auth.repository.UserRepository;
import com.auth.repository.AddressRepository; // 🟢 ADDED
import com.auth.response.PartnerInfoDto;
import com.auth.model.Address;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth/internal")
@RequiredArgsConstructor
public class InternalAuthController {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;

    @PostMapping("/customer-ids")
    public Map<Long, String> getCustomerIdsBulk(@RequestBody List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return new HashMap<>();

        List<Object[]> results = userRepository.findCustomerIdsByUserIds(userIds);
        Map<Long, String> customerIdMap = new HashMap<>();

        for (Object[] row : results) {
            Long numericId = (Long) row[0];
            String customerId = (String) row[1];
            customerIdMap.put(numericId, customerId);
        }
        return customerIdMap;
    }

    @PostMapping("/partner-details")
    public Map<Long, PartnerInfoDto> getPartnerDetailsBulk(@RequestBody List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return new HashMap<>();

        List<Object[]> userResults = userRepository.findPartnerDetailsByIds(userIds);

        List<Address> activeAddresses = addressRepository.findByUserIdsAndStatusBulk(userIds, "active");

        Map<Long, String> userAddressMap = activeAddresses.stream()
                .filter(a -> a.getUser() != null && a.getUser().getId() != null)
                .collect(Collectors.toMap(
                        a -> a.getUser().getId(),
                        a -> a.getAreaStreetCityBlockDetails() != null ? a.getAreaStreetCityBlockDetails() : "Address not provided",
                        (existing, replacement) -> existing // Keep first match if multiple exist
                ));

        Map<Long, PartnerInfoDto> partnerMap = new HashMap<>();

        for (Object[] row : userResults) {
            Long id = (Long) row[0];
            String name = (String) row[1];

            String addressStr = userAddressMap.getOrDefault(id, "Address not provided");

            partnerMap.put(id, new PartnerInfoDto(
                    name != null ? name : "Unknown Partner",
                    addressStr
            ));
        }
        return partnerMap;
    }
}