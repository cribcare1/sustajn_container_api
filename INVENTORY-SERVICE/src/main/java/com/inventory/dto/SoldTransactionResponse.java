package com.inventory.dto;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SoldTransactionResponse {
    private String id;                         // System profile identity number
    private String type;                       // Set to either "USER" or "RESTAURANT"
    private String name;                       // Populates either Profile Name or Customer ID heading
    private String customerId;                 // Only populated for USER (e.g., "JACK-1234"), null for RESTAURANT
    private String address;                    // Only populated for RESTAURANT, null for USER
    private String formattedDate;              // Displays card timeline entry "21.11.2025"
    private Integer totalQuantity;             // Total icon index summary (e.g., 3 items)
    private Integer totalAmount;               // Total price tag on the card block (e.g., 300)
    private String productCodesConcatenated;   // Subheader text string: "ST-DC-50 | ST-RDC-500"
    private List<SoldContainerItemDetail> containers; // Mapped inner list array for bottom sheets
}