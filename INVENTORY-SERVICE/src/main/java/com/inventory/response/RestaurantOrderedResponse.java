package com.inventory.response;

import com.inventory.Constant.AdminOrderStatus;
import com.inventory.Constant.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RestaurantOrderedResponse {

    private String productName; //fetch from ContainerType entity
    private String orderId;        // reference to order service order ID
    private String orderDate; // dd/MM/yyyy HH:mm am/pm
    private TransactionType type;     // BORROW / RETURN
    private String status;   // PENDING / APPROVED / REJECTED
    private String restaurantRemark;    // message from restaurant
    private String adminRemark;        // rejection/approval message
    private String decisionAt;  // when admin acted (dd/MM/yyyy HH:mm am/pm)
    private Integer requestedQty;     // restaurant requested quantity
    private Integer approvedQty;      // admin approved qty

    public RestaurantOrderedResponse(
            String productName,
            String orderId,
            LocalDateTime orderDate,
            TransactionType type,
            AdminOrderStatus status,
            String restaurantRemark,
            String adminRemark,
            LocalDateTime decisionAt,
            Integer requestedQty,
            Integer approvedQty
    ) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a");

        this.productName = productName;
        this.orderId = orderId;
        this.orderDate = orderDate != null ? orderDate.format(formatter) : null;
        this.type = type;
        this.status = status != null ? status.name() : null;
        this.restaurantRemark = restaurantRemark;
        this.adminRemark = adminRemark;
        this.decisionAt = decisionAt != null ? decisionAt.format(formatter) : null;
        this.requestedQty = requestedQty;
        this.approvedQty = approvedQty;
    }
}
