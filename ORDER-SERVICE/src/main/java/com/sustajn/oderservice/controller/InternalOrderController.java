package com.sustajn.oderservice.controller;

import com.sustajn.oderservice.dto.CustomerSoldHistoryRawDto;
import com.sustajn.oderservice.repository.SoldOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/internal/orders")
@RequiredArgsConstructor
public class InternalOrderController {

    private final SoldOrderRepository soldOrderRepository;

    @GetMapping("/customer/sold-history-raw/{userId}")
    public ResponseEntity<List<CustomerSoldHistoryRawDto>> getCustomerSoldHistoryRaw(@PathVariable("userId") Long userId) {
        List<Object[]> rawRows = soldOrderRepository.findCustomerSoldHistoryRawDataNative(userId);
        List<CustomerSoldHistoryRawDto> resultList = new ArrayList<>();

        for (Object[] row : rawRows) {
            resultList.add(CustomerSoldHistoryRawDto.builder()
                    .productId(row[0] != null ? ((Number) row[0]).longValue() : null)
                    .soldQuantity(row[1] != null ? ((Number) row[1]).intValue() : 0)
                    .unitPrice(row[2] != null ? ((Number) row[2]).longValue() : 0L)
                    .totalAmount(row[3] != null ? ((Number) row[3]).longValue() : 0L)
                    .borrowedAt(row[4] != null ? ((Timestamp) row[4]).toLocalDateTime() : null)
                    .dueDate(row[5] != null ? ((Timestamp) row[5]).toLocalDateTime() : null)
                    .soldAt(row[6] != null ? ((Timestamp) row[6]).toLocalDateTime() : null)
                    .build());
        }

        return ResponseEntity.ok(resultList);
    }
}