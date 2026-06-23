package com.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingOrderRequestResponse {
    private Long id;
    private String requestNumber;      // Maps to order.getOrderId() (e.g., "ORD-234")
    private String requestType;        // Displays string representations ("BORROW" / "RETURN")
    private String restaurantName;     // Loaded dynamically via your AuthFeignClient map parser
    private String containerCodes;     // Pip-separated product IDs (e.g., "ST-DC-50 | ST-RDC-500")
    private String formattedDateTime;  // Converted timeline tag string (e.g., "27.11.2025 | 11:00")
    private Integer totalQuantity;     // Aggregated total container units count sum
    private List<String> imageUrls;    // Array of string thumbnail locations for visual slots
}