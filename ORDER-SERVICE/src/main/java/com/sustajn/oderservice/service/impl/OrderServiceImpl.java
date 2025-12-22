package com.sustajn.oderservice.service.impl;

import com.sustajn.oderservice.entity.BorrowOrder;
import com.sustajn.oderservice.entity.Order;
import com.sustajn.oderservice.entity.ReturnOrder;
import com.sustajn.oderservice.repository.BorrowOrderRepository;
import com.sustajn.oderservice.repository.OrderRepository;
import com.sustajn.oderservice.repository.ReturnOrderRepository;
import com.sustajn.oderservice.request.BorrowItemRequest;
import com.sustajn.oderservice.request.BorrowRequest;
import com.sustajn.oderservice.request.ReturnItemRequest;
import com.sustajn.oderservice.request.ReturnRequest;
import com.sustajn.oderservice.service.OrderService;
import com.sustajn.oderservice.util.ApiResponseUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final BorrowOrderRepository borrowOrderRepository;
    private final ReturnOrderRepository returnOrderRepository;
    @Override
    @Transactional
    public Map<String, Object> borrowContainers(BorrowRequest request) {
        try {

            validateBorrowRequest(request);

            // 1. Create Order internally
            Order order = new Order();
            order.setUserId(request.getUserId());
            order.setOrderDate(LocalDateTime.now());
            order.setTransactionId(UUID.randomUUID().toString());

            orderRepository.save(order);

            // 2. Create Borrow Orders
            for (BorrowItemRequest item : request.getItems()) {

                BorrowOrder borrowOrder = new BorrowOrder();
                borrowOrder.setOrderId(order.getId());
                borrowOrder.setRestaurantId(request.getRestaurantId());
                borrowOrder.setUserId(request.getUserId());
                borrowOrder.setProductId(item.getProductId());
                borrowOrder.setQuantity(item.getQuantity());
                borrowOrder.setReturnedQuantity(0);
                borrowOrder.setBorrowedAt(LocalDateTime.now());
                borrowOrder.setDueDate(LocalDateTime.now().plusDays(7));

                borrowOrderRepository.save(borrowOrder);
            }

            return ApiResponseUtil.success("Containers borrowed successfully");

        } catch (Exception ex) {
            return handleBorrowError(ex);
        }
    }
    private Map<String, Object> handleBorrowError(Exception ex) {


        return ApiResponseUtil.error(
                ex.getMessage() != null ? ex.getMessage() : "Failed to borrow containers"
        );
    }
    private void validateBorrowRequest(BorrowRequest request) {

        if (request.getUserId() == null) {
            throw new IllegalArgumentException("UserId is required");
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Borrow items cannot be empty");
        }

        for (BorrowItemRequest item : request.getItems()) {
            if (item.getProductId() == null || item.getQuantity() <= 0) {
                throw new IllegalArgumentException(
                        "Invalid productId or quantity"
                );
            }
        }
    }

    @Override
    @Transactional
    public Map<String, Object> returnContainers(ReturnRequest request) {

        try {

            // 1. Collect productIds
            List<Long> productIds = request.getItems()
                    .stream()
                    .map(ReturnItemRequest::getProductId)
                    .distinct()
                    .toList();

            // 2. Fetch ALL pending borrows at once
            List<BorrowOrder> pendingBorrows =
                    borrowOrderRepository.findAllPendingBorrowsFIFO(
                            request.getUserId(),
                            productIds
                    );

            // 3. Group by productId
            Map<Long, List<BorrowOrder>> borrowsByProduct =
                    pendingBorrows.stream()
                            .collect(Collectors.groupingBy(
                                    BorrowOrder::getProductId,
                                    LinkedHashMap::new,
                                    Collectors.toList()
                            ));

            // ✅ Collect return orders here
            List<ReturnOrder> returnOrdersToSave = new ArrayList<>();

            // 4. Process each return item
            for (ReturnItemRequest item : request.getItems()) {

                int returnQty = item.getQuantity();

                List<BorrowOrder> borrows =
                        borrowsByProduct.getOrDefault(
                                item.getProductId(),
                                Collections.emptyList()
                        );

                for (BorrowOrder borrow : borrows) {

                    int pending = borrow.getQuantity() - borrow.getReturnedQuantity();
                    if (pending <= 0) continue;

                    int used = Math.min(returnQty, pending);

                    // Update BorrowOrder (in-memory)
                    borrow.setReturnedQuantity(
                            borrow.getReturnedQuantity() + used
                    );

                    // Create ReturnOrder (collect only)
                    ReturnOrder returnOrder = new ReturnOrder();
                    returnOrder.setBorrowOrderId(borrow.getId());
                    returnOrder.setUserId(request.getUserId());
                    returnOrder.setRestaurantId(request.getRestaurantId());
                    returnOrder.setProductId(item.getProductId());
                    returnOrder.setReturnedQuantity(used);
                    returnOrder.setReturnedAt(LocalDateTime.now());

                    returnOrdersToSave.add(returnOrder);

                    returnQty -= used;
                    if (returnQty == 0) break;
                }

                // Validation
                if (returnQty > 0) {
                    throw new IllegalArgumentException(
                            "Return quantity exceeds borrowed quantity for productId="
                                    + item.getProductId()
                    );
                }
            }

            // 5. Batch save ReturnOrders
            returnOrderRepository.saveAll(returnOrdersToSave);

            // 6. Batch update BorrowOrders
            borrowOrderRepository.saveAll(pendingBorrows);

            return ApiResponseUtil.success("Containers returned successfully");

        } catch (Exception ex) {
            return handleReturnError(ex);
        }
    }
    private Map<String, Object> handleReturnError(Exception ex) {

        return ApiResponseUtil.error(
                ex.getMessage() != null
                        ? ex.getMessage()
                        : "Failed to return containers"
        );
    }

}
