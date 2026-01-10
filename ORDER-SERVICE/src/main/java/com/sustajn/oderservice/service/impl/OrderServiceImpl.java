package com.sustajn.oderservice.service.impl;

import com.inventory.response.RestaurantOrderedResponse;
import com.sustajn.oderservice.constant.OrderServiceConstant;
import com.sustajn.oderservice.dto.*;
import com.sustajn.oderservice.entity.BorrowOrder;
import com.sustajn.oderservice.entity.Order;
import com.sustajn.oderservice.entity.ReturnOrder;
import com.sustajn.oderservice.exception.ResourceNotFoundException;
import com.sustajn.oderservice.feign.service.AuthClient;
import com.sustajn.oderservice.feign.service.InventoryFeignClient;
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

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final BorrowOrderRepository borrowOrderRepository;
    private final ReturnOrderRepository returnOrderRepository;
    private final AuthClient authClient;
    private final InventoryFeignClient inventoryFeignClient;
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

    @Override
    public Map<String, Object> getOrderDetailsListByStatusForUser(Long userId, String status) {

        Map<String, Object> response = new HashMap<>();

        try {
            // 1️⃣ Fetch Borrow Orders for user + status
            List<BorrowOrder> borrowOrders =
                    borrowOrderRepository.getAllTheApprovedBorrowOrdersByUserId(userId);

            if (borrowOrders == null || borrowOrders.isEmpty()) {
                response.put("status", "success");
                response.put("message", "No borrowed containers found for user");
                response.put("data", List.of());
                return response;
            }

            // 2️⃣ Collect product + restaurant IDs
            List<Integer> productIds = borrowOrders.stream()
                    .map(b -> b.getProductId().intValue())
                    .distinct()
                    .collect(Collectors.toList());   // ✅ mutable


            List<Long> restaurantIds = borrowOrders.stream()
                    .map(BorrowOrder::getRestaurantId)
                    .distinct()
                    .collect(Collectors.toList());   // ✅ mutable

            // 3️⃣ Call Inventory Service
            List<ProductResponse> products = List.of();
            try {
                products = inventoryFeignClient.getProductsByIds(productIds);
            } catch (Exception ex) {
                throw new RuntimeException("Failed to fetch product details from Inventory Service", ex);
            }

            Map<Long, ProductResponse> productMap = products.stream()
                    .collect(Collectors.toMap(
                            p -> p.getProductId().longValue(),
                            p -> p,
                            (a, b) -> a
                    ));

            // 4️⃣ Call Auth Service
            List<RestaurantRegisterResponse> restaurants = List.of();
            try {
                restaurants = authClient.getRestaurantsByIds(restaurantIds);
            } catch (Exception ex) {
                throw new RuntimeException("Failed to fetch restaurant details from Auth Service", ex);
            }



            Map<Long, RestaurantRegisterResponse> restaurantMap = restaurants.stream()
                    .collect(Collectors.toMap(
                            RestaurantRegisterResponse::getRestaurantId,
                            r -> r,
                            (a, b) -> a
                    ));

            // 5️⃣ Build Response List
            List<OrderDetailsResponse> results = borrowOrders.stream()
                    .map(b -> OrderDetailsResponse.builder()
                            .restaurantId(
                                    b.getRestaurantId() != null
                                            ? b.getRestaurantId().intValue()
                                            : null
                            )
                            .restaurantName(
                                    restaurantMap.containsKey(b.getRestaurantId())
                                            ? restaurantMap.get(b.getRestaurantId()).getName()
                                            : null
                            )
//                            .restaurantAddress(
//                                    restaurantMap.containsKey(b.getRestaurantId())
//                                            ? restaurantMap.get(b.getRestaurantId()).get()
//                                            : null
//                            )
                            .productId(b.getProductId())
                            .productName(
                                    productMap.containsKey(b.getProductId())
                                            ? productMap.get(b.getProductId()).getProductName()
                                            : null
                            )
                            .quantity(b.getQuantity())
                            .build()
                    )
                    .collect(Collectors.toList());   // ✅ mutable

            // 6️⃣ Final Success Response
            response.put("status", "success");
            response.put("message", "Borrowed container list fetched successfully");
            response.put("data", results);

            return response;
        }
        catch (IllegalArgumentException ex) {
            response.put("status", "error");
            response.put("message", "Invalid input provided");
            response.put("details", ex.getMessage());
            return response;
        }
        catch (RuntimeException ex) {
            response.put("status", "error");
            response.put("message", ex.getMessage());
            response.put("details", ex.getCause() != null ? ex.getCause().getMessage() : null);
            return response;
        }
        catch (Exception ex) {
            response.put("status", "error");
            response.put("message", "Unexpected error while fetching user order details");
            response.put("details", ex.getMessage());
            return response;
        }
    }

    @Override
    public ApiResponse<OrderHistoryResponse> getOrderHistory(Long restaurantId) {
        try {

            // ================= FETCH LOCAL DATA =================
            List<BorrowOrder> borrowOrders = borrowOrderRepository.findByRestaurantId(restaurantId);
            List<ReturnOrder> returnOrders = returnOrderRepository.findByRestaurantId(restaurantId);

            // ================= FETCH ORDERED DATA FROM INVENTORY =================
            ApiResponse<List<RestaurantOrderedResponse>> orderedApiResponse =
                    inventoryFeignClient.getOrderHistory(restaurantId);

            List<RestaurantOrderedResponse> orderedResponses =
                    orderedApiResponse != null && orderedApiResponse.getData() != null
                            ? orderedApiResponse.getData()
                            : new ArrayList<>();

            // ================= FETCH ORDER ENTITIES =================
            Set<Long> orderIds = borrowOrders.stream()
                    .map(BorrowOrder::getOrderId)
                    .collect(Collectors.toSet());

            List<Order> orders = orderRepository.findAllById(orderIds);

            Map<Long, String> orderTransactionMap = orders.stream()
                    .collect(Collectors.toMap(Order::getId, Order::getTransactionId));

            // ================= BUILD LOOKUP MAPS =================
            Map<Long, BorrowOrder> borrowById = borrowOrders.stream()
                    .collect(Collectors.toMap(BorrowOrder::getId, b -> b));

            Set<Integer> productIds = borrowOrders.stream()
                    .map(b -> b.getProductId().intValue())
                    .collect(Collectors.toSet());

            Map<Long, String> productNameMap = inventoryFeignClient
                    .getProductsByIds(new ArrayList<>(productIds))
                    .stream()
                    .collect(Collectors.toMap(
                            p -> p.getProductId().longValue(),
                            ProductResponse::getProductName
                    ));

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy|hh:mm a");

            // ================= LEASED SECTION =================
            Map<Long, List<BorrowOrder>> leasedGrouped =
                    borrowOrders.stream().collect(Collectors.groupingBy(BorrowOrder::getOrderId));

            List<LeasedResponse> leasedResponses = new ArrayList<>();

            for (Map.Entry<Long, List<BorrowOrder>> entry : leasedGrouped.entrySet()) {

                Long orderId = entry.getKey();
                String transactionId = orderTransactionMap.get(orderId);

                List<BorrowOrder> list = entry.getValue();

                String products = list.stream()
                        .map(b -> productNameMap.get(b.getProductId()))
                        .distinct()
                        .collect(Collectors.joining("|"));

                int totalQty = list.stream()
                        .mapToInt(BorrowOrder::getQuantity)
                        .sum();

                String dateTime = list.get(0).getBorrowedAt().format(formatter);

                leasedResponses.add(
                        new LeasedResponse(products, orderId, transactionId, dateTime, totalQty)
                );
            }

            // ================= RECEIVED SECTION =================
            Map<Long, List<ReturnOrder>> returnedGrouped =
                    returnOrders.stream().collect(Collectors.groupingBy(
                            r -> borrowById.get(r.getBorrowOrderId()).getOrderId()
                    ));

            List<ReceivedResponse> receivedResponses = new ArrayList<>();

            for (Map.Entry<Long, List<ReturnOrder>> entry : returnedGrouped.entrySet()) {

                Long orderId = entry.getKey();
                String transactionId = orderTransactionMap.get(orderId);

                List<ReturnOrder> returns = entry.getValue();
                List<BorrowOrder> relatedBorrows = leasedGrouped.get(orderId);

                String products = relatedBorrows.stream()
                        .map(b -> productNameMap.get(b.getProductId()))
                        .distinct()
                        .collect(Collectors.joining("|"));

                int totalReturnedQty = returns.stream()
                        .mapToInt(ReturnOrder::getReturnedQuantity)
                        .sum();

                String dateTime = returns.get(0).getReturnedAt().format(formatter);

                receivedResponses.add(
                        new ReceivedResponse(products, orderId, transactionId, dateTime, totalReturnedQty)
                );
            }

            // ================= FINAL RESPONSE =================
            OrderHistoryResponse response =
                    new OrderHistoryResponse(leasedResponses, receivedResponses, orderedResponses);

            return new ApiResponse<>("Order history fetched successfully",
                    OrderServiceConstant.STATUS_SUCCESS, response);

        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse<>("Failed to fetch order history",
                    OrderServiceConstant.STATUS_ERROR, null);
        }
    }


    private Map<String, Object> handleReturnError(Exception ex) {

        return ApiResponseUtil.error(
                ex.getMessage() != null
                        ? ex.getMessage()
                        : "Failed to return containers"
        );
    }

    @Override
    public Map<String, Object> getMonthWiseOrders(Long userId, int year) {

        Map<String, Object> response = new HashMap<>();

        try {
            int currentMonth = LocalDate.now().getMonthValue();

            // 1️⃣ Fetch orders up to current month
            List<BorrowOrder> borrowOrders =
                    borrowOrderRepository.findAllByUserIdAndYear(userId, year)
                            .stream()
                            .filter(b -> b.getBorrowedAt().getMonthValue() <= currentMonth)
                            .collect(Collectors.toList());

            // 2️⃣ Month map in DESCENDING order  (Dec → Nov → … → Jan)
            Map<String, List<OrderListDetails>> monthWiseOrders = new LinkedHashMap<>();
            for (int m = currentMonth; m >= 1; m--) {
                String monthName =
                        Month.of(m).getDisplayName(TextStyle.FULL, Locale.ENGLISH); // e.g., "December"
                monthWiseOrders.put(monthName, new ArrayList<>());
            }

            if (!borrowOrders.isEmpty()) {

                // 3️⃣ Collect ids
                List<Integer> productIds = borrowOrders.stream()
                        .map(b -> b.getProductId().intValue())
                        .distinct().toList();

                List<Long> restaurantIds = borrowOrders.stream()
                        .map(BorrowOrder::getRestaurantId)
                        .distinct().toList();

                // 4️⃣ Product service
                List<ProductResponse> products = inventoryFeignClient.getProductsByIds(productIds);
                Map<Long, ProductResponse> productMap = products.stream()
                        .collect(Collectors.toMap(p -> p.getProductId().longValue(), p -> p));

                // 5️⃣ Restaurant service
                List<RestaurantRegisterResponse> restaurants = authClient.getRestaurantsByIds(restaurantIds);
                Map<Long, RestaurantRegisterResponse> restaurantMap = restaurants.stream()
                        .collect(Collectors.toMap(RestaurantRegisterResponse::getRestaurantId, r -> r));

                // 6️⃣ Group by orderId
                Map<Long, List<BorrowOrder>> grouped =
                        borrowOrders.stream().collect(Collectors.groupingBy(BorrowOrder::getOrderId));

                // 7️⃣ Build order entries
                for (Map.Entry<Long, List<BorrowOrder>> entry : grouped.entrySet()) {

                    List<BorrowOrder> orderItems = entry.getValue();
                    BorrowOrder first = orderItems.get(0);

                    RestaurantRegisterResponse restaurant =
                            restaurantMap.get(first.getRestaurantId());

                    List<ProductOrderListResponse> productList = orderItems.stream()
                            .map(b -> {
                                ProductResponse p = productMap.get(b.getProductId());
                                return new ProductOrderListResponse(
                                        b.getProductId().intValue(),
                                        p != null ? p.getProductName() : null,
                                        p != null ? p.getCapacity() : null,
                                        b.getQuantity(),
                                        p != null ? p.getProductImageUrl() : null,
                                        p!= null ? p.getProductUniqueId() : null
                                );
                            })
                            .toList();

                    int totalContainers = orderItems.stream()
                            .mapToInt(BorrowOrder::getQuantity)
                            .sum();

                    LocalDateTime dt = first.getBorrowedAt();
                    String monthName =
                            Month.of(dt.getMonthValue())
                                    .getDisplayName(TextStyle.FULL, Locale.ENGLISH);

                    OrderListDetails details = OrderListDetails.builder()
                            .orderId(first.getOrderId())
                            .restaurantId(first.getRestaurantId())
                            .restaurantName(restaurant != null ? restaurant.getName() : null)
                            .restaurantAddress(restaurant!= null ? restaurant.getName() : null)
                            .productCount(productList.size())
                            .totalContainerCount(totalContainers)
                            .orderDate(dt.toLocalDate().toString())
                            .orderTime(dt.toLocalTime().toString())
                            .productOrderListResponseList(productList)
                            .build();

                    monthWiseOrders.get(monthName).add(details);
                }
            }

            response.put("status", "success");
            response.put("message", "Month-wise orders fetched successfully");
            response.put("value", monthWiseOrders);
            return response;

        } catch (Exception ex) {

            response.put("status", "error");
            response.put("message", "Failed to fetch month-wise orders");
            response.put("value", null);
            return response;
        }
    }
    @Override
    public Map<String, Object> getOrderDetailsByOrderId(Long orderId) {

        Map<String, Object> response = new HashMap<>();

        try {
            // 1️⃣ Fetch all order items for this orderId
            List<BorrowOrder> orderItems =
                    borrowOrderRepository.findAllByOrderId(orderId);

            if (orderItems == null || orderItems.isEmpty()) {
                response.put("status", "error");
                response.put("message", "No order details found for given orderId");
                response.put("data", null);
                return response;
            }

            BorrowOrder first = orderItems.get(0);

            // 2️⃣ Collect product + restaurant ids
            List<Integer> productIds = orderItems.stream()
                    .map(b -> b.getProductId().intValue())
                    .distinct()
                    .toList();

            List<Long> restaurantIds = List.of(first.getRestaurantId());

            // 3️⃣ Fetch Product Details
            List<ProductResponse> products =
                    inventoryFeignClient.getProductsByIds(productIds);

            Map<Long, ProductResponse> productMap = products.stream()
                    .collect(Collectors.toMap(
                            p -> p.getProductId().longValue(),
                            p -> p
                    ));

            // 4️⃣ Fetch Restaurant Details
            List<RestaurantRegisterResponse> restaurants =
                    authClient.getRestaurantsByIds(restaurantIds);

            RestaurantRegisterResponse restaurant =
                    restaurants.isEmpty() ? null : restaurants.get(0);

            // 5️⃣ Build product list response
            List<ProductOrderListResponse> productList = orderItems.stream()
                    .map(b -> {
                        ProductResponse p = productMap.get(b.getProductId());
                        return new ProductOrderListResponse(
                                b.getProductId().intValue(),
                                p != null ? p.getProductName() : null,
                                p != null ? p.getCapacity() : null,
                                b.getQuantity(),
                                p != null ? p.getProductImageUrl() : null,
                                p != null ? p.getProductUniqueId() : null
                        );
                    })
                    .toList();

            int totalContainers = orderItems.stream()
                    .mapToInt(BorrowOrder::getQuantity)
                    .sum();

            // 6️⃣ Final Response Object (same structure as before)
            OrderListDetails details = OrderListDetails.builder()
                    .orderId(first.getOrderId())
                    .restaurantId(first.getRestaurantId())
                    .restaurantName(restaurant != null ? restaurant.getName() : null)
                    .restaurantAddress(restaurant != null ? restaurant.getAddress() : null)
                    .productCount(productList.size())
                    .totalContainerCount(totalContainers)
                    .orderDate(first.getBorrowedAt().toLocalDate().toString())
                    .orderTime(first.getBorrowedAt().toLocalTime().toString())
                    .productOrderListResponseList(productList)
                    .build();

            response.put("status", "success");
            response.put("message", "Order details fetched successfully");
            response.put("data", details);
            return response;
        }
        catch (Exception ex) {

            response.put("status", "error");
            response.put("message", "Failed to fetch order details");
            response.put("data", null);
            return response;
        }
    }


    @Override
    public Map<String,Object> approveOrder(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        // If already approved — no update needed
        if ("APPROVED".equalsIgnoreCase(order.getOrderStatus())) {
            throw new RuntimeException("Order is already APPROVED");
        }

        // Allow only pending → approved
        if (!"PENDING".equalsIgnoreCase(order.getOrderStatus())) {
            throw new RuntimeException("Only PENDING orders can be approved");
        }

        order.setOrderStatus("APPROVED");
        orderRepository.save(order);

        Map<String,Object> response = new HashMap<>();
        response.put("status","success");
        response.put("message", "Order status updated to APPROVED");
        return response;
    }


    @Override
    public Map<String, Object> getMonthWiseReturnOrders(Long userId, int year) {

        Map<String, Object> response = new HashMap<>();

        try {
            int currentMonth = LocalDate.now().getMonthValue();
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");

            // 1️⃣ Fetch return orders up to current month
            List<ReturnOrder> returnOrders =
                    returnOrderRepository.findAllByUserIdAndYear(userId, year)
                            .stream()
                            .filter(r -> r.getReturnedAt().getMonthValue() <= currentMonth)
                            .collect(Collectors.toList());

            // 2️⃣ Month map in DESC order (Dec -> … -> Jan)
            Map<String, List<OrderListDetails>> monthWiseReturns = new LinkedHashMap<>();
            for (int m = currentMonth; m >= 1; m--) {
                String monthName = Month.of(m)
                        .getDisplayName(TextStyle.FULL, Locale.ENGLISH);
                monthWiseReturns.put(monthName, new ArrayList<>());
            }

            if (!returnOrders.isEmpty()) {

                // 3️⃣ Collect product + restaurant ids
                List<Integer> productIds = returnOrders.stream()
                        .map(r -> r.getProductId().intValue())
                        .distinct().toList();

                List<Long> restaurantIds = returnOrders.stream()
                        .map(ReturnOrder::getRestaurantId)
                        .distinct().toList();

                // 4️⃣ Product service
                List<ProductResponse> products =
                        inventoryFeignClient.getProductsByIds(productIds);

                Map<Long, ProductResponse> productMap = products.stream()
                        .collect(Collectors.toMap(
                                p -> p.getProductId().longValue(), p -> p));

                // 5️⃣ Restaurant service
                List<RestaurantRegisterResponse> restaurants =
                        authClient.getRestaurantsByIds(restaurantIds);

                Map<Long, RestaurantRegisterResponse> restaurantMap = restaurants.stream()
                        .collect(Collectors.toMap(
                                RestaurantRegisterResponse::getRestaurantId, r -> r));

                // 6️⃣ Group by borrowOrderId
                Map<Long, List<ReturnOrder>> grouped =
                        returnOrders.stream()
                                .collect(Collectors.groupingBy(ReturnOrder::getBorrowOrderId));

                // 7️⃣ Build response objects
                for (Map.Entry<Long, List<ReturnOrder>> entry : grouped.entrySet()) {

                    List<ReturnOrder> items = entry.getValue();
                    ReturnOrder first = items.get(0);

                    RestaurantRegisterResponse restaurant =
                            restaurantMap.get(first.getRestaurantId());

                    List<ProductOrderListResponse> productList = items.stream()
                            .map(r -> {
                                ProductResponse p = productMap.get(r.getProductId());
                                return new ProductOrderListResponse(
                                        r.getProductId().intValue(),
                                        p != null ? p.getProductName() : null,
                                        p != null ? p.getCapacity() : null,
                                        r.getReturnedQuantity(),
                                        p != null ? p.getProductImageUrl() : null,
                                        p != null ? p.getProductUniqueId() : null
                                );
                            })
                            .toList();

                    int totalReturned = items.stream()
                            .mapToInt(ReturnOrder::getReturnedQuantity)
                            .sum();

                    LocalDateTime dt = first.getReturnedAt();
                    String monthName = Month.of(dt.getMonthValue())
                            .getDisplayName(TextStyle.FULL, Locale.ENGLISH);

                    OrderListDetails details = OrderListDetails.builder()
                            .orderId(first.getBorrowOrderId())
                            .restaurantId(first.getRestaurantId())
                            .restaurantName(restaurant != null ? restaurant.getName() : null)
                            .restaurantAddress(restaurant != null ? restaurant.getAddress() : null)
                            .productCount(productList.size())
                            .totalContainerCount(totalReturned)
                            .orderDate(dt.toLocalDate().toString())
                            .orderTime(dt.toLocalTime().format(timeFormatter)) // ⭐ AM/PM format
                            .productOrderListResponseList(productList)
                            .build();

                    monthWiseReturns.get(monthName).add(details);
                }
            }

            response.put("status", "success");
            response.put("message", "Month-wise return orders fetched successfully");
            response.put("value", monthWiseReturns);
            return response;

        } catch (Exception ex) {
            response.put("status", "error");
            response.put("message", "Failed to fetch month-wise return orders");
            response.put("value", null);
            return response;
        }
    }


    @Override
    public Map<String, Object> getBorrowedProductSummary(Long userId) {

        Map<String, Object> response = new HashMap<>();

        try {

            List<Object[]> rows = borrowOrderRepository.getProductBorrowReturnSummary(userId);

            if (rows == null || rows.isEmpty()) {
                response.put("status", "success");
                response.put("message", "No borrowed products found for user");
                response.put("value", Collections.emptyList());
                return response;
            }

            // 🔹 Collect all unique product IDs
            Set<Integer> productIds = rows.stream()
                    .map(r -> ((Number) r[1]).intValue())
                    .collect(Collectors.toSet());

            // 🔹 Fetch product details in ONE call
            Map<Long, ProductResponse> productMap = new HashMap<>();
            try {
                List<ProductResponse> products =
                        inventoryFeignClient.getProductsByIds(new ArrayList<>(productIds));

                if (products != null) {
                    productMap = products.stream()
                            .collect(Collectors.toMap(
                                    p -> p.getProductId().longValue(),
                                    p -> p
                            ));
                }

            } catch (Exception e) {
                // Don’t fail the summary if product service is down
                productMap = Collections.emptyMap();
            }

            List<ProductDetailsResponse> result = new ArrayList<>();

            for (Object[] r : rows) {

                Long orderId     = ((Number) r[0]).longValue();
                Long productId   = ((Number) r[1]).longValue();
                int borrowedQty  = ((Number) r[2]).intValue();
                int returnedQty  = ((Number) r[3]).intValue();
                int remainingQty = ((Number) r[4]).intValue();
                Timestamp ts = (Timestamp) r[5];
                LocalDateTime orderDate = ts != null ? ts.toLocalDateTime() : null;

                // 🧮 Ensure non-negative
                if (remainingQty < 0) remainingQty = 0;

                // 🗓 Days Left (7-day rule)
                long daysPassed = ChronoUnit.DAYS.between(
                        orderDate.toLocalDate(), LocalDate.now()
                );
                long daysLeft = Math.max(0, 7 - daysPassed);

                // 🎯 Get product from map
                ProductResponse p = productMap.get(productId);

                result.add(
                        new ProductDetailsResponse(
                                orderId,
                                productId,
                                p != null ? p.getProductName() : null,
                                remainingQty,                                   // 👈 remaining qty
                                p != null ? p.getProductImageUrl() : null,
                                daysLeft,
                                p != null ? p.getProductUniqueId() : null   ,
                                p != null ? p.getCapacity() : null  // productCode
                        )
                );
            }

            response.put("status", "success");
            response.put("message", "Borrowed product summary fetched successfully");
            response.put("value", result);
            return response;

        } catch (Exception ex) {

            response.put("status", "error");
            response.put("message", "Failed to fetch borrowed product summary");
            response.put("value", null);
            response.put("error", ex.getMessage());
            return response;
        }
    }


}
