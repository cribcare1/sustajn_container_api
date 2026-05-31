package com.auth.controller;

import com.auth.repository.UserRepository;
import com.auth.response.PartnerInfoDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth/internal")
@RequiredArgsConstructor
public class InternalAuthController {

    private final UserRepository userRepository;

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

        List<Object[]> results = userRepository.findPartnerDetailsByIds(userIds);
        Map<Long, PartnerInfoDto> partnerMap = new HashMap<>();

        for (Object[] row : results) {
            Long id = (Long) row[0];
            String name = (String) row[1];
            String address = (String) row[2];

            // Handle null values just in case
            partnerMap.put(id, new PartnerInfoDto(
                    name != null ? name : "Unknown Partner",
                    address != null ? address : "Address not provided"
            ));
        }
        return partnerMap;
    }
}