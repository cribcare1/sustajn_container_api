package com.inventory.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionTransactionResponse {
    private Long id;
    private String name;                 // Displays Restaurant Name or Customer Name
    private String userType;             // 🟢 ADDED: "CUSTOMER" or "PARTNER"
    private String restaurantAddress;    // Displays address if partner, or home address if customer
    private String planType;             // e.g., "Pay-per-use", "Premium Customer Plan"
    private BigDecimal amount;           // Fee charge amount
    private String formattedDate;
    private String groupMonthYear;       // Headings formatting (e.g., "November-2025")
}