package com.sustajn.oderservice.dto;

import lombok.*;

import java.time.LocalDate;



public interface BorrowOrderResponse {
    Long getOrderId();

    Long getProductId();

    Integer getBorrowedQty();

    Integer getReturnedQty();

    Integer getRemainingQty();

    LocalDate getDueDate();

    LocalDate getOrderDate();
}