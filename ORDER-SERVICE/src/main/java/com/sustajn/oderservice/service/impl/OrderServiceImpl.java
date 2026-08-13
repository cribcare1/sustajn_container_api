package com.sustajn.oderservice.service.impl;

import com.sustajn.oderservice.constant.OrderEnumType;
import com.sustajn.oderservice.constant.OrderServiceConstant;
import com.sustajn.oderservice.dto.*;
import com.sustajn.oderservice.entity.*;
import com.sustajn.oderservice.exception.ResourceNotFoundException;
import com.sustajn.oderservice.feign.service.AuthClient;
import com.sustajn.oderservice.feign.service.InventoryFeignClient;
import com.sustajn.oderservice.feign.service.NotificationFeignClient;
import com.sustajn.oderservice.projection.LeasedReturnedCountWithTimeGraphProjection;
import com.sustajn.oderservice.repository.BorrowOrderRepository;
import com.sustajn.oderservice.repository.OrderRepository;
import com.sustajn.oderservice.repository.ReturnOrderRepository;
import com.sustajn.oderservice.repository.SoldOrderRepository;
import com.sustajn.oderservice.request.*;
import com.sustajn.oderservice.service.OrderService;
import com.sustajn.oderservice.util.ApiResponseUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import com.sustajn.oderservice.dto.DeviceTokenResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final BorrowOrderRepository borrowOrderRepository;
    private final ReturnOrderRepository returnOrderRepository;
    private final SoldOrderRepository soldOrderRepository;
    private final AuthClient authClient;
    private final InventoryFeignClient inventoryFeignClient;
    private final NotificationFeignClient notificationFeignClient;

    private final DateTimeFormatter cardFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy | HH:mm");
    private final DateTimeFormatter headerFormatter = DateTimeFormatter.ofPattern("MMMM-yyyy");

    private final List<SseEmitter> dashboardEmitters = new CopyOnWriteArrayList<>();

//    @Override
//    @Transactional
//    public Map<String, Object> borrowContainers(BorrowRequest request) {
//        try {
//
//            validateBorrowRequest(request);
//
//            // 1. Build qty map
//            Map<Integer, Integer> qtyMap =
//                    request.getItems().stream()
//                            .collect(Collectors.groupingBy(
//                                    item -> item.getProductId().intValue(),
//                                    Collectors.summingInt(
//                                            BorrowItemRequest::getQuantity)
//                            ));
//
//            ReduceInventoryRequest inventoryRequest =
//                    new ReduceInventoryRequest();
//            inventoryRequest.setRestaurantId(request.getRestaurantId());
//            inventoryRequest.setContainerQtyMap(qtyMap);
//
//            // 🔥 CALL INVENTORY FIRST
//            Map<String, Object> response = inventoryFeignClient.checkAvailability(inventoryRequest);
//
//            if (response.get(OrderServiceConstant.STATUS).equals(OrderServiceConstant.STATUS_ERROR)){
//                System.err.println("Inventory check failed: " + response);
//                return ApiResponseUtil.error(
//                        response.get(OrderServiceConstant.MESSAGE) != null
//                                ? response.get(OrderServiceConstant.MESSAGE).toString()
//                                : "Inventory check failed"
//                );
//            }
//
//            // 2. Only if inventory is OK → create order
//            Order order = new Order();
//            order.setUserId(request.getUserId());
//            order.setOrderDate(LocalDateTime.now());
//            order.setTransactionId(UUID.randomUUID().toString());
//            order.setOrderStatus(OrderServiceConstant.PENDING);
//            orderRepository.save(order);
//
//            // 3. Create borrow rows
//            for (BorrowItemRequest item : request.getItems()) {
//
//                BorrowOrder borrowOrder = new BorrowOrder();
//                borrowOrder.setOrderId(order.getId());
//                borrowOrder.setRestaurantId(request.getRestaurantId());
//                borrowOrder.setUserId(request.getUserId());
//                borrowOrder.setProductId(item.getProductId());
//                borrowOrder.setQuantity(item.getQuantity());
//                borrowOrder.setReturnedQuantity(0);
//                borrowOrder.setBorrowedAt(LocalDateTime.now());
//                borrowOrder.setDueDate(LocalDateTime.now().plusDays(7));
//
//                borrowOrderRepository.save(borrowOrder);
//            }
//
//            return ApiResponseUtil.success("Containers borrowed successfully");
//
//        } catch (Exception ex) {
//            return handleBorrowError(ex);
//        }
//    }


    @Override
    @Transactional(rollbackOn = Exception.class)
    public Map<String, Object> borrowContainers(BorrowRequest request) {
        try {

            validateBorrowRequest(request);

            // 1️⃣ Build qty map
            Map<Integer, Integer> qtyMap =
                    request.getItems().stream()
                            .collect(Collectors.groupingBy(
                                    item -> item.getProductId().intValue(),
                                    Collectors.summingInt(
                                            BorrowItemRequest::getQuantity)
                            ));

            ReduceInventoryRequest inventoryRequest =
                    new ReduceInventoryRequest();
            inventoryRequest.setRestaurantId(request.getRestaurantId());
            inventoryRequest.setContainerQtyMap(qtyMap);

            // 2️⃣ CHECK ONLY (no change)
            Map<String, Object> checkResponse =
                    inventoryFeignClient.checkAvailability(inventoryRequest);

            if (!OrderServiceConstant.STATUS_SUCCESS
                    .equalsIgnoreCase(
                            checkResponse.get(OrderServiceConstant.STATUS).toString())) {

                return ApiResponseUtil.error(
                        checkResponse.get(OrderServiceConstant.MESSAGE).toString()
                );
            }

            // 3️⃣ FINAL REDUCE
            ApiResponse<?> reduceResponse =
                    inventoryFeignClient
                            .reduceAvailableContainers(inventoryRequest);

            if (!OrderServiceConstant.STATUS_SUCCESS
                    .equalsIgnoreCase(reduceResponse.getStatus())) {

                return ApiResponseUtil.error(
                        reduceResponse.getMessage()
                );
            }

            ApiResponse<UserResponse> userResponse = authClient.getUserByCustomerId(request.getCustomerId());
            if (userResponse == null || userResponse.getData() == null) {
                throw new ResourceNotFoundException("User not found for customerId: " + request.getCustomerId());
            }

            Long userId = userResponse.getData().getId();

            // 4️⃣ Create APPROVED order
            Order order = new Order();
            order.setUserId(userId);
            order.setOrderDate(LocalDateTime.now().withSecond(0).withNano(0));
            order.setTransactionId(UUID.randomUUID().toString());
            order.setOrderStatus(OrderServiceConstant.APPROVED);
            orderRepository.save(order);

            // 5️⃣ Create borrow rows
            for (BorrowItemRequest item : request.getItems()) {

                BorrowOrder borrowOrder = new BorrowOrder();
                borrowOrder.setOrderId(order.getId());
                borrowOrder.setRestaurantId(request.getRestaurantId());
                borrowOrder.setUserId(userId);
                borrowOrder.setProductId(item.getProductId().longValue());
                borrowOrder.setQuantity(item.getQuantity());
                borrowOrder.setReturnedQuantity(0);
                borrowOrder.setBorrowedAt(LocalDateTime.now().withSecond(0).withNano(0));
                borrowOrder.setDueDate(LocalDateTime.now().plusDays(7));

                borrowOrderRepository.save(borrowOrder);
            }

            List<Integer> productIds = request.getItems()
                    .stream()
                    .map(BorrowItemRequest::getProductId)
                    .collect(Collectors.toList());

            List<ProductResponse> products =
                    inventoryFeignClient.getProductsByIds(productIds).getData();

            Map<Integer, String> productNameMap =
                    products.stream()
                            .collect(Collectors.toMap(
                                    ProductResponse::getProductId,
                                    ProductResponse::getProductName
                            ));

            System.err.println("Product name map: " + productNameMap);

            // ===============================
            // 8️⃣ Notification
            // ===============================

            DeviceTokenResponse deviceTokenResponse = notificationFeignClient.getDeviceTokensByUserId(userId);
            System.err.println("Device token response for userId " + userId + ": " + deviceTokenResponse);
            if (deviceTokenResponse != null) {

                System.err.println("Device token response for userId " + userId + ": " + deviceTokenResponse.getDeviceToken());

                String title = "Containers Borrowed Successfully";
                StringBuilder body = new StringBuilder();
                body.append("Hi ")
                        .append(userResponse.getData().getFullName())
                        .append(",\n\n")
                        .append("You have successfully borrowed the following containers:\n");

                log.error("Device token for userId {}: {}", userId, deviceTokenResponse.getDeviceToken());
                for (BorrowItemRequest item : request.getItems()) {

                    String productName =
                            productNameMap.getOrDefault(
                                    item.getProductId(),
                                    "Unknown Product");

                    body.append("- Product Name: ")
                            .append(productName)
                            .append(", Quantity: ")
                            .append(item.getQuantity())
                            .append("\n");
                }
                NotificationResponse notification = NotificationResponse.builder()
                        .title(title)
                        .body(body.toString())
                        .deviceTokens(List.of(deviceTokenResponse.getDeviceToken()))
                        .data(OrderServiceConstant.ACTION_BORROW)
                        .build();

                Notification notificationEntity = new Notification();

                notificationFeignClient.sendNotificationToMultipleDevices(notification);
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

        if (request.getCustomerId() == null || request.getCustomerId().isEmpty()) {
            throw new IllegalArgumentException("CustomerId is required");
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

            List<Long> productIds = request.getItems()
                    .stream()
                    .map(ReturnItemRequest::getProductId)
                    .distinct()
                    .toList();

            List<BorrowOrder> pendingBorrows =
                    borrowOrderRepository.findAllPendingBorrowsFIFO(
                            request.getUserId(),
                            productIds
                    );

            Map<Long, List<BorrowOrder>> borrowsByProduct =
                    pendingBorrows.stream()
                            .collect(Collectors.groupingBy(
                                    BorrowOrder::getProductId,
                                    LinkedHashMap::new,
                                    Collectors.toList()
                            ));

            List<ReturnOrder> returnOrdersToSave = new ArrayList<>();

            for (ReturnItemRequest item : request.getItems()) {

                int returnQty = item.getQuantity();

                List<BorrowOrder> borrows =
                        borrowsByProduct.getOrDefault(
                                item.getProductId(),
                                Collections.emptyList()
                        );

                for (BorrowOrder borrow : borrows) {

                    int pending =
                            borrow.getQuantity() - borrow.getReturnedQuantity();
                    if (pending <= 0) continue;

                    int used = Math.min(returnQty, pending);

                    borrow.setReturnedQuantity(
                            borrow.getReturnedQuantity() + used);

                    ReturnOrder returnOrder = new ReturnOrder();
                    returnOrder.setBorrowOrderId(borrow.getId());
                    returnOrder.setUserId(request.getUserId());
                    returnOrder.setRestaurantId(request.getRestaurantId());
                    returnOrder.setProductId(item.getProductId());
                    returnOrder.setReturnedQuantity(used);
                    returnOrder.setReturnedAt(LocalDateTime.now().withSecond(0).withNano(0));

                    returnOrdersToSave.add(returnOrder);

                    returnQty -= used;
                    if (returnQty == 0) break;
                }

                if (returnQty > 0) {
                    throw new IllegalArgumentException(
                            "Return quantity exceeds borrowed quantity "
                    );
                }
            }

            // 🔥 UPDATE INVENTORY FIRST
            Map<Integer, Integer> qtyMap =
                    request.getItems().stream()
                            .collect(Collectors.groupingBy(
                                    item -> item.getProductId().intValue(),
                                    Collectors.summingInt(
                                            ReturnItemRequest::getQuantity)
                            ));

            ReduceInventoryRequest inventoryRequest =
                    new ReduceInventoryRequest();
            inventoryRequest.setRestaurantId(request.getRestaurantId());
            inventoryRequest.setContainerQtyMap(qtyMap);


            ApiResponse<?> invResponse =
                    inventoryFeignClient.increaseAvailableContainers(inventoryRequest);

            if (OrderServiceConstant.STATUS_ERROR.equals(invResponse.getStatus())) {
                return ApiResponseUtil.error(
                        invResponse.getMessage());
            }


            // Commit DB only if inventory OK
            returnOrderRepository.saveAll(returnOrdersToSave);
            borrowOrderRepository.saveAll(pendingBorrows);

            DeviceTokenResponse deviceTokenResponse = notificationFeignClient.getDeviceTokensByUserId(request.getUserId());
            if (deviceTokenResponse != null) {

                String title = "Containers Returned Successfully";
                StringBuilder body = new StringBuilder();
                body.append("Hi,\n\n")
                        .append("You have successfully returned the following containers:\n");

                for (ReturnItemRequest item : request.getItems()) {

                    body.append("- Product ID: ")
                            .append(item.getProductId())
                            .append(", Quantity: ")
                            .append(item.getQuantity())
                            .append("\n");
                }
                NotificationResponse notification = NotificationResponse.builder()
                        .title(title)
                        .body(body.toString())
                        .deviceTokens(List.of(deviceTokenResponse.getDeviceToken()))
                        .data(OrderServiceConstant.ACTION_RETURN)
                        .build();

                notificationFeignClient.sendNotificationToMultipleDevices(notification);
            }


            return ApiResponseUtil.success(
                    "Containers returned successfully");

        } catch (Exception ex) {
            ex.printStackTrace();
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
                products = inventoryFeignClient.getProductsByIds(productIds).getData();
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
        } catch (IllegalArgumentException ex) {
            response.put("status", "error");
            response.put("message", "Invalid input provided");
            response.put("details", ex.getMessage());
            return response;
        } catch (RuntimeException ex) {
            response.put("status", "error");
            response.put("message", ex.getMessage());
            response.put("details", ex.getCause() != null ? ex.getCause().getMessage() : null);
            return response;
        } catch (Exception ex) {
            response.put("status", "error");
            response.put("message", "Unexpected error while fetching user order details");
            response.put("details", ex.getMessage());
            return response;
        }
    }

    @Override
    public ApiResponse<PartnerGraphBridgeResponse> getPartnerDailyGraph(Long restaurantId, String monthName, int year, Long productId) {
        try {
            // 1. Parse string Month Name to Month Enum safely
            Month monthEnum;
            try {
                monthEnum = Month.valueOf(monthName.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return new ApiResponse<>("ERROR", "Invalid month name provided: " + monthName, null);
            }

            // 2. Set temporal boundaries for the target month
            YearMonth yearMonth = YearMonth.of(year, monthEnum);
            LocalDateTime startDate = yearMonth.atDay(1).atStartOfDay();
            LocalDateTime endDate = yearMonth.atEndOfMonth().atTime(23, 59, 59);

            String label = monthEnum.getDisplayName(TextStyle.FULL, Locale.ENGLISH) + "-" + year;

            // 🟢 FIX HERE: If productId is 0, turn it into null so the SQL query fetches ALL products combined
            Long effectiveProductId = (productId != null && productId == 0) ? null : productId;

            // 3. Query dataset aggregates using the effectiveProductId variable
            List<Object[]> leasedData = borrowOrderRepository.getDailyLeasedQuantities(restaurantId, effectiveProductId, startDate, endDate);
            List<Object[]> returnedData = returnOrderRepository.getDailyReturnedQuantities(restaurantId, effectiveProductId, startDate, endDate);

            // 4. Transform native records to fast-lookup maps (Day -> Count)
            Map<Integer, Integer> leasedMap = new HashMap<>();
            for (Object[] row : leasedData) {
                leasedMap.put(((Number) row[0]).intValue(), ((Number) row[1]).intValue());
            }

            Map<Integer, Integer> returnedMap = new HashMap<>();
            for (Object[] row : returnedData) {
                returnedMap.put(((Number) row[0]).intValue(), ((Number) row[1]).intValue());
            }

            // 5. Construct a continuous daily matrix for the frontend chart grid
            List<PartnerGraphBridgeResponse.DailyStat> dailyStatsList = new ArrayList<>();
            int daysInMonth = yearMonth.lengthOfMonth();

            for (int day = 1; day <= daysInMonth; day++) {
                String dayOfWeekName = yearMonth.atDay(day).getDayOfWeek()
                        .getDisplayName(TextStyle.SHORT, Locale.ENGLISH);

                dailyStatsList.add(new PartnerGraphBridgeResponse.DailyStat(
                        day,
                        dayOfWeekName,
                        leasedMap.getOrDefault(day, 0),
                        returnedMap.getOrDefault(day, 0)
                ));
            }

            PartnerGraphBridgeResponse responseData = PartnerGraphBridgeResponse.builder()
                    .monthYear(label)
                    .dailyStats(dailyStatsList)
                    .build();

            return new ApiResponse<>("SUCCESS", "Graph metrics processed successfully", responseData);

        } catch (Exception e) {
            log.error("Failed to generate chart statistics aggregate details: ", e);
            return new ApiResponse<>("ERROR", "Internal system logic failure", null);
        }
    }

    @Override
    public ApiResponse<UserDetailsInsightsResponse> getUserDetailsDashboardInsights(Long userId) {
        try {
            LocalDateTime currentTime = LocalDateTime.now();

            // 1. Fetch KPI Status Metrics
            Integer activeCount = borrowOrderRepository.countActiveContainersByUserId(userId);
            Integer overdueCount = borrowOrderRepository.countOverdueContainersByUserId(userId, currentTime);

            // 2. Compute Borrow Insights Data
            List<Object[]> borrowRecords = borrowOrderRepository.getUserProductBorrowTotals(userId);
            UserDetailsInsightsResponse.UserProductStat mostBorrowed = null;
            UserDetailsInsightsResponse.UserProductStat lessBorrowed = null;

            if (borrowRecords != null && !borrowRecords.isEmpty()) {
                long totalBorrows = 0;
                Long maxId = null;
                long maxCount = Long.MIN_VALUE;
                Long minId = null;
                long minCount = Long.MAX_VALUE;

                for (Object[] row : borrowRecords) {
                    Long prodId = ((Number) row[0]).longValue();
                    long qty = ((Number) row[1]).longValue();
                    totalBorrows += qty;

                    if (qty > maxCount) {
                        maxCount = qty;
                        maxId = prodId;
                    }
                    if (qty < minCount) {
                        minCount = qty;
                        minId = prodId;
                    }
                }

                if (totalBorrows > 0) {
                    int maxPct = (int) Math.round((double) maxCount / totalBorrows * 100);
                    int minPct = (int) Math.round((double) minCount / totalBorrows * 100);

                    mostBorrowed = buildUserProductStat(maxId, maxPct);
                    if (!maxId.equals(minId)) {
                        lessBorrowed = buildUserProductStat(minId, minPct);
                    }
                }
            }

            // 3. Compute Return Insights Data
            List<Object[]> returnRecords = returnOrderRepository.getUserProductReturnTotals(userId);
            UserDetailsInsightsResponse.UserProductStat mostReturn = null;
            UserDetailsInsightsResponse.UserProductStat lessReturn = null;

            if (returnRecords != null && !returnRecords.isEmpty()) {
                long totalReturns = 0;
                Long maxId = null;
                long maxCount = Long.MIN_VALUE;
                Long minId = null;
                long minCount = Long.MAX_VALUE;

                for (Object[] row : returnRecords) {
                    Long prodId = ((Number) row[0]).longValue();
                    long qty = ((Number) row[1]).longValue();
                    totalReturns += qty;

                    if (qty > maxCount) {
                        maxCount = qty;
                        maxId = prodId;
                    }
                    if (qty < minCount) {
                        minCount = qty;
                        minId = prodId;
                    }
                }

                if (totalReturns > 0) {
                    int maxPct = (int) Math.round((double) maxCount / totalReturns * 100);
                    int minPct = (int) Math.round((double) minCount / totalReturns * 100);

                    mostReturn = buildUserProductStat(maxId, maxPct);
                    if (!maxId.equals(minId)) {
                        lessReturn = buildUserProductStat(minId, minPct);
                    }
                }
            }

            // 4. Combine Final Payload Dataset
            UserDetailsInsightsResponse responsePayload = UserDetailsInsightsResponse.builder()
                    .activeCount(activeCount)
                    .overdueCount(overdueCount)
                    .mostBorrowed(mostBorrowed)
                    .lessBorrowed(lessBorrowed)
                    .mostReturn(mostReturn)
                    .lessReturn(lessReturn)
                    .build();

            return new ApiResponse<>("SUCCESS", "User dashboard insights processed successfully", responsePayload);

        } catch (Exception e) {
            log.error("Failed to compile layout insights for user ID: {}", userId, e);
            return new ApiResponse<>("ERROR", "Internal system metrics collection failure", null);
        }
    }

    // Reuse Feign Lookup Engine Mapper to resolve product names/codes/images
    // Reuse Feign Lookup Engine Mapper to resolve product names/codes/images
    private UserDetailsInsightsResponse.UserProductStat buildUserProductStat(
            Long productId,
            int percentage) {

        String name = "Unknown Container";
        String code = "N/A";
        String capacity = "0ml";
        String imageUrl = null;

        try {

            if (productId != null) {

                Integer containerTypeId = productId.intValue();

                ApiResponse<ContainerTypeResponse> inventoryResponse =
                        inventoryFeignClient.getContainerTypeById(containerTypeId);

                if (inventoryResponse != null && inventoryResponse.getData() != null) {

                    ContainerTypeResponse container = inventoryResponse.getData();

                    name = container.getName();
                    code = container.getProductId();
                    imageUrl = container.getImageUrl();

                    if (container.getCapacityMl() != null) {
                        capacity = container.getCapacityMl() + "ml";
                    }
                }
            }

        } catch (Exception ex) {
            log.error(
                    "Feign lookup exception on user details resolving ID: {}",
                    productId,
                    ex
            );
        }

        return UserDetailsInsightsResponse.UserProductStat.builder()
                .productId(productId)
                .productName(name)
                .productCode(code)
                .capacity(capacity)
                .imageUrl(imageUrl)
                .percentage(percentage)
                .build();
    }

    @Override
    public ApiResponse<MostAndLeastLeasedResponse> getMostAndLeastLeasedContainer(Long restaurantId) {

        try {

            List<Object[]> records =
                    borrowOrderRepository.getProductLeasedTotalsLifetime(restaurantId);

            if (records == null || records.isEmpty()) {
                return new ApiResponse<>(
                        "SUCCESS",
                        "No lifetime leasing data found for this partner",
                        null
                );
            }

            long totalLeasedSum = 0;
            Long mostLeasedId = null;
            long highestCount = Long.MIN_VALUE;

            Long leastLeasedId = null;
            long lowestCount = Long.MAX_VALUE;

            for (Object[] row : records) {

                Long productId = ((Number) row[0]).longValue();
                long count = ((Number) row[1]).longValue();

                totalLeasedSum += count;

                if (count > highestCount) {
                    highestCount = count;
                    mostLeasedId = productId;
                }

                if (count < lowestCount) {
                    lowestCount = count;
                    leastLeasedId = productId;
                }
            }

            if (totalLeasedSum == 0 || mostLeasedId == null || leastLeasedId == null) {
                return new ApiResponse<>(
                        "SUCCESS",
                        "No container tracking volume found",
                        null
                );
            }

            int maxPercentage =
                    (int) Math.round((double) highestCount / totalLeasedSum * 100);

            int minPercentage =
                    (int) Math.round((double) lowestCount / totalLeasedSum * 100);

            MostAndLeastLeasedResponse.ProductLeasedStat mostStat =
                    buildLiveProductStat(mostLeasedId, maxPercentage);

            MostAndLeastLeasedResponse.ProductLeasedStat leastStat = null;

            if (!mostLeasedId.equals(leastLeasedId)) {
                leastStat = buildLiveProductStat(leastLeasedId, minPercentage);
            }

            MostAndLeastLeasedResponse finalResult =
                    MostAndLeastLeasedResponse.builder()
                            .mostLeased(mostStat)
                            .lessLeased(leastStat)
                            .build();

            return new ApiResponse<>(
                    "SUCCESS",
                    "Lifetime leasing insights processed successfully",
                    finalResult
            );

        } catch (Exception e) {

            log.error(
                    "Failed to compile lifetime asset metrics highlight summary",
                    e
            );

            return new ApiResponse<>(
                    "ERROR",
                    "Internal system computation failure",
                    null
            );
        }
    }


    // Helper method to fetch container details from inventory service
    private MostAndLeastLeasedResponse.ProductLeasedStat buildLiveProductStat(
            Long productId,
            int percentage) {

        String name = "Unknown Container";
        String code = "N/A";
        String capacity = "0ml";
        String imageUrl = null;

        try {

            if (productId != null) {

                Integer containerTypeId = productId.intValue();

                ApiResponse<ContainerTypeResponse> inventoryResponse =
                        inventoryFeignClient.getContainerTypeById(containerTypeId);

                if (inventoryResponse != null && inventoryResponse.getData() != null) {

                    ContainerTypeResponse container = inventoryResponse.getData();

                    name = container.getName();
                    code = container.getProductId();
                    imageUrl = container.getImageUrl();

                    if (container.getCapacityMl() != null) {
                        capacity = container.getCapacityMl() + "ml";
                    }
                }
            }

        } catch (Exception ex) {

            log.error(
                    "Feign communication failure looking up asset specification for ID: {}",
                    productId,
                    ex
            );
        }

        return MostAndLeastLeasedResponse.ProductLeasedStat.builder()
                .productId(productId)
                .productName(name)
                .productCode(code)
                .capacity(capacity)
                .imageUrl(imageUrl)
                .percentage(percentage)
                .build();
    }

    @Override
    public ExtensionPreviewResponse getExtensionPreview(Long orderId) {
        List<BorrowOrder> borrowOrders = borrowOrderRepository.findByOrderId(orderId);
        if (borrowOrders.isEmpty()) {
            throw new RuntimeException("No active borrow items found for orderId: " + orderId);
        }

        // 1. Filter out items that are already fully returned
        List<BorrowOrder> itemsToExtend = borrowOrders.stream()
                .filter(bo -> bo.getReturnedQuantity() < bo.getQuantity())
                .collect(Collectors.toList());

        if (itemsToExtend.isEmpty()) {
            throw new RuntimeException("All items in this order have already been returned.");
        }

        // 2. Resolve timestamps from the first available active item row
        BorrowOrder referenceItem = itemsToExtend.get(0);
        LocalDateTime currentDue = referenceItem.getEffectiveDueDate() != null
                ? referenceItem.getEffectiveDueDate()
                : referenceItem.getDueDate();
        LocalDateTime newDue = currentDue.plusDays(5); // Appends the standard 5-day lease cushion

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        // 3. Gather component container keys to perform the microservice bridge lookup
        List<Integer> containerTypeIds = itemsToExtend.stream()
                .map(bo -> bo.getProductId().intValue())
                .distinct()
                .collect(Collectors.toList());

        // 4. Query the Inventory Service via Feign
        List<Map<String, Object>> remoteDetails = inventoryFeignClient.getContainerExtensionDetailsBulk(containerTypeIds);
        Map<Integer, Map<String, Object>> metadataMap = remoteDetails.stream()
                .collect(Collectors.toMap(m -> (Integer) m.get("id"), m -> m, (a, b) -> a));

        // 🟢 Standalone top-level collection declaration
        List<ExtensionItemDetail> productList = new ArrayList<>();
        BigDecimal grandTotalFee = BigDecimal.ZERO;

        // 5. Aggregate active balances and multiply unit cost rates against item counts
        for (BorrowOrder bo : itemsToExtend) {
            Integer targetId = bo.getProductId().intValue();
            int remainingQty = bo.getQuantity() - bo.getReturnedQuantity();

            String name = "Unknown Container";
            String code = "N/A";
            String capacity = "0ml";
            String imageUrl = "";
            BigDecimal unitFee = BigDecimal.ZERO;

            if (metadataMap.containsKey(targetId)) {
                Map<String, Object> meta = metadataMap.get(targetId);
                if (meta.get("name") != null) name = meta.get("name").toString();
                if (meta.get("productId") != null) code = meta.get("productId").toString();
                if (meta.get("capacityMl") != null) capacity = meta.get("capacityMl").toString() + "ml";
                if (meta.get("imageUrl") != null) imageUrl = meta.get("imageUrl").toString();
                if (meta.get("extendFee") != null) unitFee = new BigDecimal(meta.get("extendFee").toString());
            }

            // Extended Item Price Calculation = Unit Fee * Remaining Quantity
            BigDecimal itemTotalFee = unitFee.multiply(BigDecimal.valueOf(remainingQty));
            grandTotalFee = grandTotalFee.add(itemTotalFee);

            // 🟢 Clean standalone class builder invocation (Zero IntelliJ compiler lag)
            productList.add(ExtensionItemDetail.builder()
                    .containerTypeId(bo.getProductId())
                    .containerName(name)
                    .productCode(code)
                    .capacity(capacity)
                    .imageUrl(imageUrl)
                    .quantityToExtend(remainingQty)
                    .build());
        }

        return ExtensionPreviewResponse.builder()
                .currentDueDate(currentDue.format(dtf))
                .newDueDate(newDue.format(dtf))
                .totalExtensionFee(grandTotalFee)
                .products(productList)
                .build();
    }
    @Override
    public List<ExtendedFeeMonthWiseResponse> getGlobalExtendedFeeDashboard() {
        List<ExtendedFeeMonthWiseResponse> payload = new ArrayList<>();

        try {
            // 1. Fetch all system-wide lease extension records
            List<BorrowOrder> customerExtendedRows = borrowOrderRepository.findByIsExtendedTrueAndExtendedFeeIsNotNullOrderByExtendedAtDesc();
            if (customerExtendedRows.isEmpty()) {
                return payload;
            }

            // 2. 🟢 FIXED: Extract all distinct customer user IDs directly from the rows
            List<Long> customerUserIds = customerExtendedRows.stream()
                    .map(BorrowOrder::getUserId)
                    .distinct()
                    .collect(Collectors.toList());

            Map<Long, String> customerIdMap = new HashMap<>();

            // 3. Batch look up the customer text identifiers from AUTH-SERVICE
            try {
                if (!customerUserIds.isEmpty()) {
                    Map<Long, String> rawCustomers = authClient.getCustomerIdsBulk(customerUserIds);
                    if (rawCustomers != null) {
                        for (Map.Entry<?, String> entry : rawCustomers.entrySet()) {
                            Long userIdKey = Long.valueOf(entry.getKey().toString());
                            customerIdMap.put(userIdKey, entry.getValue());
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Feign microservice exception batch fetching customer identity strings: ", e);
            }

            // 4. Group by Order ID to aggregate items extended together inside the same transaction window
            Map<Long, List<BorrowOrder>> ordersMap = customerExtendedRows.stream()
                    .collect(Collectors.groupingBy(BorrowOrder::getOrderId, LinkedHashMap::new, Collectors.toList()));

            List<ExtendedFeeTransactionResponse> flatCardsList = new ArrayList<>();
            Map<Long, LocalDateTime> timeCache = new HashMap<>();

            for (Map.Entry<Long, List<BorrowOrder>> entry : ordersMap.entrySet()) {
                Long orderId = entry.getKey();
                List<BorrowOrder> items = entry.getValue();
                BorrowOrder firstItem = items.get(0);

                LocalDateTime actionTime = firstItem.getExtendedAt() != null ? firstItem.getExtendedAt() : LocalDateTime.now();
                timeCache.put(orderId, actionTime);

                // 🟢 FIXED: Use getQuantity() directly to prevent 0 values if containers are returned later
                int totalExtendedQty = items.stream().mapToInt(BorrowOrder::getQuantity).sum();

                BigDecimal totalOrderFee = items.stream()
                        .map(bo -> bo.getExtendedFee() != null ? bo.getExtendedFee() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Map to real customer identifier strings (e.g. "ALOK-230626")
                String profileName = "JACK-" + firstItem.getUserId();
                if (customerIdMap.containsKey(firstItem.getUserId())) {
                    profileName = customerIdMap.get(firstItem.getUserId());
                }

                flatCardsList.add(ExtendedFeeTransactionResponse.builder()
                        .orderId(orderId)
                        .name(profileName)
                        .formattedDateTime(actionTime.format(cardFormatter))
                        .totalQuantity(totalExtendedQty)
                        .totalAmount(totalOrderFee)
                        .build());
            }

            // 5. Partition elements into chronological monthly dashboard segments
            Map<String, List<ExtendedFeeTransactionResponse>> monthlyBuckets = flatCardsList.stream()
                    .collect(Collectors.groupingBy(
                            tx -> timeCache.get(tx.getOrderId()).format(headerFormatter),
                            LinkedHashMap::new, Collectors.toList()
                    ));

            for (Map.Entry<String, List<ExtendedFeeTransactionResponse>> bucket : monthlyBuckets.entrySet()) {
                BigDecimal monthSum = bucket.getValue().stream()
                        .map(ExtendedFeeTransactionResponse::getTotalAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                payload.add(ExtendedFeeMonthWiseResponse.builder()
                        .monthYear(bucket.getKey())
                        .monthTotalAmount(monthSum)
                        .transactions(bucket.getValue())
                        .build());
            }

        } catch (Exception ex) {
            log.error("Unhandled runtime failure packaging global transactions tab payload: ", ex);
        }
        return payload;
    }

    @Override
    public List<UserExtendedFeeMonthWiseResponse> getUserExtendedFeeHistory(Long userId) {
        List<UserExtendedFeeMonthWiseResponse> payload = new ArrayList<>();

        try {
            List<BorrowOrder> userRows = borrowOrderRepository.findByUserIdAndIsExtendedTrueAndExtendedFeeIsNotNullOrderByExtendedAtDesc(userId);
            if (userRows.isEmpty()) {
                return payload;
            }

            Map<Long, List<BorrowOrder>> ordersMap = userRows.stream()
                    .collect(Collectors.groupingBy(BorrowOrder::getOrderId, LinkedHashMap::new, Collectors.toList()));

            List<UserExtendedFeeItemResponse> flatUserItems = new ArrayList<>();
            Map<Long, LocalDateTime> timeCache = new HashMap<>();

            for (Map.Entry<Long, List<BorrowOrder>> entry : ordersMap.entrySet()) {
                Long orderId = entry.getKey();
                List<BorrowOrder> items = entry.getValue();
                BorrowOrder firstItem = items.get(0);

                LocalDateTime actionTime = firstItem.getExtendedAt() != null ? firstItem.getExtendedAt() : LocalDateTime.now();
                timeCache.put(orderId, actionTime);

                // 🟢 FIXED: Use getQuantity() directly to accurately reflect item extension numbers
                int totalQty = items.stream().mapToInt(BorrowOrder::getQuantity).sum();

                BigDecimal totalFee = items.stream()
                        .map(bo -> bo.getExtendedFee() != null ? bo.getExtendedFee() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                flatUserItems.add(UserExtendedFeeItemResponse.builder()
                        .orderId(orderId)
                        .formattedDateTime(actionTime.format(cardFormatter))
                        .totalQuantity(totalQty)
                        .totalAmount(totalFee)
                        .build());
            }

            Map<String, List<UserExtendedFeeItemResponse>> monthlyBuckets = flatUserItems.stream()
                    .collect(Collectors.groupingBy(
                            item -> timeCache.get(item.getOrderId()).format(headerFormatter),
                            LinkedHashMap::new, Collectors.toList()
                    ));

            for (Map.Entry<String, List<UserExtendedFeeItemResponse>> bucket : monthlyBuckets.entrySet()) {
                BigDecimal userMonthSum = bucket.getValue().stream()
                        .map(UserExtendedFeeItemResponse::getTotalAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                payload.add(UserExtendedFeeMonthWiseResponse.builder()
                        .monthYear(bucket.getKey())
                        .monthTotalAmount(userMonthSum)
                        .extensions(bucket.getValue())
                        .build());
            }

        } catch (Exception ex) {
            log.error("Unhandled runtime exception gathering individual context maps: ", ex);
        }
        return payload;
    }
    @Override
    public AdminDashboardResponse getAdminDashboardMetrics() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startOfToday = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
            LocalDateTime startOfYesterday = startOfToday.minusDays(1);

            // 1. CIRCULATION CALCULATION
            List<BorrowOrder> nonSoldOrders = borrowOrderRepository.findByIsSoldFalse();
            int totalCirculation = 0, activeContainers = 0, overdueContainers = 0;
            BigDecimal totalExtendedFee = BigDecimal.ZERO;

            for (BorrowOrder bo : nonSoldOrders) {
                int outstandingQty = bo.getQuantity() - bo.getReturnedQuantity();
                if (outstandingQty > 0) {
                    totalCirculation += outstandingQty;
                    LocalDateTime absoluteDueDate = bo.getEffectiveDueDate() != null ? bo.getEffectiveDueDate() : bo.getDueDate();
                    if (now.isAfter(absoluteDueDate)) {
                        overdueContainers += outstandingQty;
                    } else {
                        activeContainers += outstandingQty;
                    }
                }
                if (bo.getExtendedFee() != null) {
                    totalExtendedFee = totalExtendedFee.add(bo.getExtendedFee());
                }
            }

            // SOLD REVENUE CALCULATION FROM REAL TRANSACTIONS
            List<BorrowOrder> soldOrders = borrowOrderRepository.findAll().stream().filter(BorrowOrder::getIsSold).toList();
            BigDecimal totalSoldRevenue = BigDecimal.ZERO;
            for (BorrowOrder bo : soldOrders) {
                if (bo.getExtendedFee() != null) {
                    totalSoldRevenue = totalSoldRevenue.add(bo.getExtendedFee().multiply(new BigDecimal(bo.getQuantity())));
                }
            }

            // 2. TODAY VS YESTERDAY QUANTITY TREND CALCULATIONS
            List<BorrowOrder> todayBorrows = borrowOrderRepository.findBorrowsBetweenDates(startOfToday, now);
            List<BorrowOrder> yesterdayBorrows = borrowOrderRepository.findBorrowsBetweenDates(startOfYesterday, startOfToday);
            int leasedTodayCount = todayBorrows.stream().mapToInt(BorrowOrder::getQuantity).sum();
            int leasedYesterdayCount = yesterdayBorrows.stream().mapToInt(BorrowOrder::getQuantity).sum();
            String leasedTrend = calculateTrend(leasedTodayCount, leasedYesterdayCount);

            List<ReturnOrder> todayReturns = returnOrderRepository.findReturnsBetweenDates(startOfToday, now);
            List<ReturnOrder> yesterdayReturns = returnOrderRepository.findReturnsBetweenDates(startOfYesterday, startOfToday);
            int returnedTodayCount = todayReturns.stream().mapToInt(ReturnOrder::getReturnedQuantity).sum();
            int returnedYesterdayCount = yesterdayReturns.stream().mapToInt(ReturnOrder::getReturnedQuantity).sum();
            String returnsTrend = calculateTrend(returnedTodayCount, returnedYesterdayCount);

            // 3. UNIQUE USER INTERACTIONS TRACKER
            Set<Long> uniqueUsersToday = new HashSet<>();
            todayBorrows.forEach(b -> uniqueUsersToday.add(b.getUserId()));
            todayReturns.forEach(r -> uniqueUsersToday.add(r.getUserId()));

            // 4. REAL OPERATION LIFESPAN METRICS
            List<ReturnOrder> allPastReturns = returnOrderRepository.findAll();
            double totalReturnDays = 0; long totalReturnQuantitySum = 0;
            List<Long> parentBorrowIds = allPastReturns.stream().map(ReturnOrder::getBorrowOrderId).distinct().toList();
            Map<Long, BorrowOrder> borrowOrderMap = borrowOrderRepository.findAllById(parentBorrowIds).stream()
                    .collect(Collectors.toMap(BorrowOrder::getId, b -> b, (b1, b2) -> b1));

            for (ReturnOrder ro : allPastReturns) {
                BorrowOrder parent = borrowOrderMap.get(ro.getBorrowOrderId());
                if (parent != null && parent.getBorrowedAt() != null && ro.getReturnedAt() != null) {
                    long daysBetween = Duration.between(parent.getBorrowedAt(), ro.getReturnedAt()).toDays();
                    totalReturnDays += (daysBetween * ro.getReturnedQuantity());
                    totalReturnQuantitySum += ro.getReturnedQuantity();
                }
            }
            double avgReturnTime = totalReturnQuantitySum > 0 ? (totalReturnDays / totalReturnQuantitySum) : 0.0;

            // 5. POPULARITY SHIFTS EVALUATION AND BULK SERVICE FEIGN CROSS-REFERENCES
            List<BorrowOrder> allHistoricalBorrows = borrowOrderRepository.findAll();
            Map<Long, Integer> productDistributionMap = allHistoricalBorrows.stream()
                    .collect(Collectors.groupingBy(BorrowOrder::getProductId, Collectors.summingInt(BorrowOrder::getQuantity)));
            int overallLeaseVolume = productDistributionMap.values().stream().mapToInt(Integer::intValue).sum();

            PopularProductInfo mostLeasedCard = null; PopularProductInfo lessLeasedCard = null;

            if (!productDistributionMap.isEmpty() && overallLeaseVolume > 0) {
                Long maxId = Collections.max(productDistributionMap.entrySet(), Map.Entry.comparingByValue()).getKey();
                Long minId = Collections.min(productDistributionMap.entrySet(), Map.Entry.comparingByValue()).getKey();

                int maxPct = (productDistributionMap.get(maxId) * 100) / overallLeaseVolume;
                int minPct = (productDistributionMap.get(minId) * 100) / overallLeaseVolume;

                try {
                    List<Integer> targetIds = List.of(maxId.intValue(), minId.intValue());
                    List<ProductResponse> products = inventoryFeignClient.getProductsByIds(targetIds).getData();
                    Map<Long, ProductResponse> productMap = products.stream()
                            .collect(Collectors.toMap(p -> p.getProductId().longValue(), p -> p, (p1, p2) -> p1));

                    ProductResponse maxProd = productMap.get(maxId);
                    if (maxProd != null) {
                        mostLeasedCard = PopularProductInfo.builder().productId(maxId).name(maxProd.getProductName())
                                .productCode(maxProd.getProductUniqueId()).capacity(maxProd.getCapacity() != null ? maxProd.getCapacity() + "ml" : "0ml").percentage(maxPct).build();
                    }
                    ProductResponse minProd = productMap.get(minId);
                    if (minProd != null) {
                        lessLeasedCard = PopularProductInfo.builder().productId(minId).name(minProd.getProductName())
                                .productCode(minProd.getProductUniqueId()).capacity(minProd.getCapacity() != null ? minProd.getCapacity() + "ml" : "0ml").percentage(minPct).build();
                    }
                } catch (Exception ex) {
                    log.error("Failed fetching metadata from Inventory-Service inside stream task logic: ", ex);
                }
            }

            return AdminDashboardResponse.builder()
                    .containersCirculation(totalCirculation).activeContainers(activeContainers).overdueContainers(overdueContainers)
                    .todayLeased(leasedTodayCount).leasedTrendPercentage(leasedTrend).todayReturns(returnedTodayCount).returnsTrendPercentage(returnsTrend)
                    .extendedFeeRevenue(totalExtendedFee).soldRevenue(totalSoldRevenue).averageReturnTimeDays(Math.round(avgReturnTime * 10.0) / 10.0)
                    .activeUsersToday(uniqueUsersToday.size()).mostLeased(mostLeasedCard).lessLeased(lessLeasedCard).build();

        } catch (Exception ex) {
            log.error("Failed dynamic compilation inside processing stream metrics: ", ex);
            throw new RuntimeException("Operational engine database failure context logs parsing exception.");
        }
    }

    private String calculateTrend(int today, int yesterday) {
        if (yesterday == 0) return "+0%";
        int difference = today - yesterday;
        double percentage = ((double) difference / yesterday) * 100;
        return (percentage >= 0 ? "+" : "") + Math.round(percentage) + "%";
    }

    @Override
    public ApiResponse<List<SoldHistoryMonthGroupResponse>> getSoldHistoryGroupedByMonth(
            String userType, Long productId, String searchKeyword) {

        try {
            boolean isUserType = "USER".equalsIgnoreCase(userType);

            List<SoldOrder> soldOrders = isUserType
                    ? soldOrderRepository.findUserSoldOrders(productId)
                    : soldOrderRepository.findRestaurantSoldOrders(productId);

            if (soldOrders.isEmpty()) {
                return new ApiResponse<>("SUCCESS", "No sold records found", List.of());
            }

            // 1. Fetch real product codes from INVENTORY-SERVICE using containerTypeIds
            List<Integer> containerTypeIds = soldOrders.stream()
                    .map(SoldOrder::getProductId)
                    .filter(Objects::nonNull)
                    .map(Long::intValue)
                    .distinct()
                    .toList();

            Map<Integer, String> containerCodeMap = new HashMap<>();
            if (!containerTypeIds.isEmpty()) {
                try {
                    Map<Integer, String> res = inventoryFeignClient.getContainerProductCodesBulk(containerTypeIds);
                    if (res != null) {
                        containerCodeMap.putAll(res);
                    }
                } catch (Exception ex) {
                    log.error("Failed to fetch container product codes from INVENTORY-SERVICE: {}", ex.getMessage());
                }
            }

            Map<Long, String> customerIdMap = new HashMap<>();
            Map<Long, String> restaurantNameMap = new HashMap<>();

            // 2. Fetch real details from AUTH-SERVICE via AuthClient
            if (isUserType) {
                // Fetch Customer IDs for User Tab
                List<Long> userIds = soldOrders.stream()
                        .map(SoldOrder::getUserId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();

                if (!userIds.isEmpty()) {
                    try {
                        Map<Long, String> res = authClient.getCustomerIdsBulk(userIds);
                        if (res != null) customerIdMap.putAll(res);
                    } catch (Exception ex) {
                        log.error("Failed to fetch customer IDs from AUTH-SERVICE: {}", ex.getMessage());
                    }
                }
            } else {
                // Fetch Restaurant Names for Partner Tab
                List<Long> restaurantIds = soldOrders.stream()
                        .map(SoldOrder::getRestaurantId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();

                if (!restaurantIds.isEmpty()) {
                    try {
                        // Priority 1: Try partner-details bulk endpoint
                        Map<Long, PartnerInfoDto> partnerDetails = authClient.getPartnerDetailsBulk(restaurantIds);
                        if (partnerDetails != null) {
                            partnerDetails.forEach((id, info) -> {
                                if (info != null && StringUtils.hasText(info.getName())) {
                                    restaurantNameMap.put(id, info.getName());
                                }
                            });
                        }
                    } catch (Exception ex) {
                        log.warn("Partner details bulk call failed, trying getRestaurantsByIds fallback: {}", ex.getMessage());
                        try {
                            // Priority 2: Fallback to getRestaurantsByIds
                            List<RestaurantRegisterResponse> restaurants = authClient.getRestaurantsByIds(restaurantIds);
                            if (restaurants != null) {
                                for (RestaurantRegisterResponse r : restaurants) {
                                    if (r.getRestaurantId() != null && StringUtils.hasText(r.getName())) {
                                        restaurantNameMap.put(r.getRestaurantId(), r.getName());
                                    }
                                }
                            }
                        } catch (Exception e) {
                            log.error("Failed to fetch restaurant names from AUTH-SERVICE fallback: {}", e.getMessage());
                        }
                    }
                }
            }

            DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMMM-yyyy", Locale.ENGLISH);
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

            // 3. Group records chronologically by Month-Year
            Map<String, List<SoldOrder>> monthGroupedMap = soldOrders.stream()
                    .filter(s -> s.getSoldAt() != null)
                    .collect(Collectors.groupingBy(
                            s -> s.getSoldAt().format(monthFormatter),
                            LinkedHashMap::new,
                            Collectors.toList()
                    ));

            List<SoldHistoryMonthGroupResponse> monthGroupList = new ArrayList<>();

            for (Map.Entry<String, List<SoldOrder>> entry : monthGroupedMap.entrySet()) {
                String monthYear = entry.getKey();
                List<SoldOrder> ordersInMonth = entry.getValue();

                List<SoldHistoryItemResponse> items = new ArrayList<>();

                for (SoldOrder so : ordersInMonth) {

                    // Resolve Customer Code or default
                    String custId = isUserType && so.getUserId() != null
                            ? customerIdMap.getOrDefault(so.getUserId(), "USER-" + so.getUserId())
                            : null;

                    // Resolve Partner/Restaurant Name or default
                    String partnerName = !isUserType && so.getRestaurantId() != null
                            ? restaurantNameMap.getOrDefault(so.getRestaurantId(), "Partner #" + so.getRestaurantId())
                            : null;

                    // Resolve Real Container Code
                    Integer containerTypeId = so.getProductId() != null ? so.getProductId().intValue() : null;
                    String realContainerCode = containerTypeId != null
                            ? containerCodeMap.getOrDefault(containerTypeId, "N/A")
                            : "N/A";

                    // Search keyword filtering
                    if (StringUtils.hasText(searchKeyword)) {
                        String keyword = searchKeyword.toLowerCase();
                        if (isUserType && (custId == null || !custId.toLowerCase().contains(keyword))) {
                            continue;
                        }
                        if (!isUserType && (partnerName == null || !partnerName.toLowerCase().contains(keyword))) {
                            continue;
                        }
                    }

                    String dateStr = so.getSoldAt().format(dateFormatter);
                    String timeStr = so.getSoldAt().format(timeFormatter);

                    items.add(SoldHistoryItemResponse.builder()
                            .id(so.getId())
                            .userId(so.getUserId())
                            .customerId(custId)
                            .restaurantId(so.getRestaurantId())
                            .restaurantName(partnerName)
                            .productId(so.getProductId())
                            .containerCode(realContainerCode) // 🟢 Real Product Code dynamically mapped
                            .soldQuantity(so.getSoldQuantity() != null ? so.getSoldQuantity() : 0)
                            .soldDate(dateStr)
                            .soldTime(timeStr)
                            .fullDateTime(dateStr + " | " + timeStr)
                            .reason(so.getReason() != null ? so.getReason() : "N/A")
                            .build());
                }

                if (!items.isEmpty()) {
                    int monthTotalQty = items.stream().mapToInt(SoldHistoryItemResponse::getSoldQuantity).sum();
                    monthGroupList.add(SoldHistoryMonthGroupResponse.builder()
                            .monthYear(monthYear)
                            .totalQuantity(monthTotalQty)
                            .items(items)
                            .build());
                }
            }

            return new ApiResponse<>("SUCCESS", "Sold history fetched successfully", monthGroupList);

        } catch (Exception e) {
            log.error("Error fetching sold history for userType {}: {}", userType, e.getMessage(), e);
            return new ApiResponse<>("ERROR", "Failed to fetch sold history", List.of());
        }
    }
//    public ApiResponse<OrderHistoryResponse> getOrderHistory(Long restaurantId) {
//        try {
//
//            // ================= FETCH LOCAL DATA =================
//            List<BorrowOrder> borrowOrders = borrowOrderRepository.findByRestaurantId(restaurantId);
//            List<ReturnOrder> returnOrders = returnOrderRepository.findByRestaurantId(restaurantId);
//
//            // ================= FETCH ORDERED DATA FROM INVENTORY =================
//            ApiResponse<List<RestaurantOrderedResponse>> orderedApiResponse =
//                    inventoryFeignClient.getOrderHistory(restaurantId);
//
//            List<RestaurantOrderedResponse> orderedResponses =
//                    orderedApiResponse != null && orderedApiResponse.getData() != null
//                            ? orderedApiResponse.getData()
//                            : new ArrayList<>();
//
//            // ================= FETCH ORDER ENTITIES =================
//            Set<Long> orderIds = borrowOrders.stream()
//                    .map(BorrowOrder::getOrderId)
//                    .collect(Collectors.toSet());
//
//            List<Order> orders = orderRepository.findAllById(orderIds);
//
//            Map<Long, String> orderTransactionMap = orders.stream()
//                    .collect(Collectors.toMap(Order::getId, Order::getTransactionId));
//
//            // ================= BUILD LOOKUP MAPS =================
//            Map<Long, BorrowOrder> borrowById = borrowOrders.stream()
//                    .collect(Collectors.toMap(BorrowOrder::getId, b -> b));
//
//            Set<Integer> productIds = borrowOrders.stream()
//                    .map(b -> b.getProductId().intValue())
//                    .collect(Collectors.toSet());
//
//            Map<Long, String> productNameMap = inventoryFeignClient
//                    .getProductsByIds(new ArrayList<>(productIds)).getData()
//                    .stream()
//                    .collect(Collectors.toMap(
//                            p -> p.getProductId().longValue(),
//                            ProductResponse::getProductName
//                    ));
//
//            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy|hh:mm a");
//
//            // ================= LEASED SECTION =================
//            Map<Long, List<BorrowOrder>> leasedGrouped =
//                    borrowOrders.stream().collect(Collectors.groupingBy(BorrowOrder::getOrderId));
//
//            List<LeasedResponse> leasedResponses = new ArrayList<>();
//
//            for (Map.Entry<Long, List<BorrowOrder>> entry : leasedGrouped.entrySet()) {
//
//                Long orderId = entry.getKey();
//                String transactionId = orderTransactionMap.get(orderId);
//                List<BorrowOrder> list = entry.getValue();
//
//
//                String products = list.stream()
//                        .map(b -> productNameMap.get(b.getProductId()))
//                        .distinct()
//                        .collect(Collectors.joining("|"));
//
//                int totalQty = list.stream().mapToInt(BorrowOrder::getQuantity).sum();
//
//                String dateTime = list.get(0).getBorrowedAt().format(formatter);
//
//                List<ProductOrderListResponse> productOrderListResponses = list.stream()
//                        .map(b -> {
//                            ProductResponse p = inventoryFeignClient
//                                    .getProductsByIds(List.of(b.getProductId().intValue())).getData()
//                                    .get(0);
//
//                            return new ProductOrderListResponse(
//                                    p.getProductId(),
//                                    p.getProductName(),
//                                    p.getCapacity(),
//                                    b.getQuantity(),                 // containerCount
//                                    p.getProductImageUrl(),
//                                    p.getProductUniqueId()
//                            );
//                        })
//                        .collect(Collectors.toList());
//
//
//                leasedResponses.add(
//                        new LeasedResponse(products, orderId, transactionId, dateTime, totalQty, productOrderListResponses)
//                );
//            }
//
//            // ================= RECEIVED SECTION =================
//            Map<Long, List<ReturnOrder>> returnedGrouped =
//                    returnOrders.stream()
//                            .filter(r -> borrowById.containsKey(r.getBorrowOrderId()))
//                            .collect(Collectors.groupingBy(
//                                    r -> borrowById.get(r.getBorrowOrderId()).getOrderId()
//                            ));
//
//            List<ReceivedResponse> receivedResponses = new ArrayList<>();
//
//            for (Map.Entry<Long, List<ReturnOrder>> entry : returnedGrouped.entrySet()) {
//
//                Long orderId = entry.getKey();
//                String transactionId = orderTransactionMap.get(orderId);
//
//                List<ReturnOrder> returns = entry.getValue();
//                List<BorrowOrder> relatedBorrows = leasedGrouped.get(orderId);
//
//                String products = relatedBorrows.stream()
//                        .map(b -> productNameMap.get(b.getProductId()))
//                        .distinct()
//                        .collect(Collectors.joining("|"));
//
//                int totalReturnedQty = returns.stream().mapToInt(ReturnOrder::getReturnedQuantity).sum();
//
//                String dateTime = returns.get(0).getReturnedAt().format(formatter);
//
//                List<ProductOrderListResponse> productOrderListResponses = relatedBorrows.stream()
//                        .map(b -> {
//                            int returnedQty = returns.stream()
//                                    .filter(r -> r.getProductId().equals(b.getProductId()))
//                                    .mapToInt(ReturnOrder::getReturnedQuantity)
//                                    .sum();
//
//                            ProductResponse p = inventoryFeignClient
//                                    .getProductsByIds(List.of(b.getProductId().intValue())).getData()
//                                    .get(0);
//
//                            return new ProductOrderListResponse(
//                                    p.getProductId(),
//                                    p.getProductName(),
//                                    p.getCapacity(),
//                                    returnedQty,                     // containerCount
//                                    p.getProductImageUrl(),
//                                    p.getProductUniqueId()
//                            );
//                        })
//                        .collect(Collectors.toList());
//
//
//                receivedResponses.add(
//                        new ReceivedResponse(products, orderId, transactionId, dateTime, totalReturnedQty, productOrderListResponses)
//                );
//            }
//
//            // ================= FINAL RESPONSE =================
//            OrderHistoryResponse response =
//                    new OrderHistoryResponse(leasedResponses, receivedResponses, orderedResponses);
//
//            return new ApiResponse<>("Order history fetched successfully",
//                    OrderServiceConstant.STATUS_SUCCESS, response);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            return new ApiResponse<>("Failed to fetch order history",
//                    OrderServiceConstant.STATUS_ERROR, null);
//        }
//    }


    @Override
    public ApiResponse<OrderHistoryResponse> getOrderHistory(Long restaurantId) {

        try {

            // ================= FETCH LOCAL DATA =================
            List<BorrowOrder> borrowOrders =
                    borrowOrderRepository.findByRestaurantId(restaurantId);

            List<ReturnOrder> returnOrders =
                    returnOrderRepository.findByRestaurantId(restaurantId);

            if (borrowOrders == null) borrowOrders = new ArrayList<>();
            if (returnOrders == null) returnOrders = new ArrayList<>();

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

            List<Order> orders = orderIds.isEmpty()
                    ? new ArrayList<>()
                    : orderRepository.findAllById(orderIds);

            Map<Long, String> orderTransactionMap = orders.stream()
                    .collect(Collectors.toMap(
                            Order::getId,
                            Order::getTransactionId
                    ));

            // ================= BUILD PRODUCT MAP (ONE FEIGN CALL ONLY) =================
            Set<Integer> productIds = borrowOrders.stream()
                    .map(b -> b.getProductId().intValue())
                    .collect(Collectors.toSet());

            List<ProductResponse> products = new ArrayList<>();

            if (!productIds.isEmpty()) {
                ApiResponse<List<ProductResponse>> productApiResponse =
                        inventoryFeignClient.getProductsByIds(new ArrayList<>(productIds));

                if (productApiResponse != null && productApiResponse.getData() != null) {
                    products = productApiResponse.getData();
                }
            }

            Map<Long, ProductResponse> productMap = products.stream()
                    .collect(Collectors.toMap(
                            p -> p.getProductId().longValue(),
                            Function.identity()
                    ));

            DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern("dd/MM/yyyy | hh:mm a");

            // ================= LEASED SECTION =================
            Map<Long, List<BorrowOrder>> leasedGrouped =
                    borrowOrders.stream()
                            .collect(Collectors.groupingBy(BorrowOrder::getOrderId));

            List<LeasedResponse> leasedResponses = new ArrayList<>();

            for (Map.Entry<Long, List<BorrowOrder>> entry : leasedGrouped.entrySet()) {

                Long orderId = entry.getKey();
                List<BorrowOrder> list = entry.getValue();

                String transactionId = orderTransactionMap.get(orderId);

                int totalQty = list.stream()
                        .mapToInt(BorrowOrder::getQuantity)
                        .sum();

                String dateTime = list.get(0).getBorrowedAt().format(formatter);

                List<ProductOrderListResponse> productList = list.stream()
                        .map(b -> {

                            ProductResponse p = productMap.get(b.getProductId());

                            return new ProductOrderListResponse(
                                    p != null ? p.getProductId() : null,
                                    p != null ? p.getProductName() : "Unknown",
                                    p != null ? p.getCapacity() : null,
                                    b.getQuantity(),
                                    p != null ? p.getProductImageUrl() : null,
                                    p != null ? p.getProductUniqueId() : null
                            );
                        })
                        .collect(Collectors.toList());

                String productsJoined = productList.stream()
                        .map(ProductOrderListResponse::getProductName)
                        .distinct()
                        .collect(Collectors.joining(" | "));

                leasedResponses.add(
                        new LeasedResponse(
                                productsJoined,
                                orderId,
                                transactionId,
                                dateTime,
                                totalQty,
                                productList
                        )
                );
            }

            // ================= RECEIVED SECTION =================
            Map<Long, BorrowOrder> borrowById = borrowOrders.stream()
                    .collect(Collectors.toMap(
                            BorrowOrder::getId,
                            Function.identity()
                    ));

            Map<Long, List<ReturnOrder>> returnedGrouped =
                    returnOrders.stream()
                            .filter(r -> borrowById.containsKey(r.getBorrowOrderId()))
                            .collect(Collectors.groupingBy(
                                    r -> borrowById.get(r.getBorrowOrderId()).getOrderId()
                            ));

            List<ReceivedResponse> receivedResponses = new ArrayList<>();

            for (Map.Entry<Long, List<ReturnOrder>> entry : returnedGrouped.entrySet()) {

                Long orderId = entry.getKey();
                List<ReturnOrder> returns = entry.getValue();

                String transactionId = orderTransactionMap.get(orderId);

                List<BorrowOrder> relatedBorrows = leasedGrouped.get(orderId);
                if (relatedBorrows == null) continue;

                int totalReturnedQty = returns.stream()
                        .mapToInt(ReturnOrder::getReturnedQuantity)
                        .sum();

                String dateTime =
                        returns.get(0).getReturnedAt().format(formatter);

                List<ProductOrderListResponse> productList = relatedBorrows.stream()
                        .map(b -> {

                            int returnedQty = returns.stream()
                                    .filter(r -> r.getProductId().equals(b.getProductId()))
                                    .mapToInt(ReturnOrder::getReturnedQuantity)
                                    .sum();

                            ProductResponse p = productMap.get(b.getProductId());

                            return new ProductOrderListResponse(
                                    p != null ? p.getProductId() : null,
                                    p != null ? p.getProductName() : "Unknown",
                                    p != null ? p.getCapacity() : null,
                                    returnedQty,
                                    p != null ? p.getProductImageUrl() : null,
                                    p != null ? p.getProductUniqueId() : null
                            );
                        })
                        .collect(Collectors.toList());

                String productsJoined = productList.stream()
                        .map(ProductOrderListResponse::getProductName)
                        .distinct()
                        .collect(Collectors.joining(" | "));

                receivedResponses.add(
                        new ReceivedResponse(
                                productsJoined,
                                orderId,
                                transactionId,
                                dateTime,
                                totalReturnedQty,
                                productList
                        )
                );
            }

            // ================= FINAL RESPONSE =================
            OrderHistoryResponse response =
                    new OrderHistoryResponse(
                            leasedResponses,
                            receivedResponses,
                            orderedResponses
                    );

            return new ApiResponse<>(
                    "Order history fetched successfully",
                    OrderServiceConstant.STATUS_SUCCESS,
                    response
            );

        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse<>(
                    "Failed to fetch order history",
                    OrderServiceConstant.STATUS_ERROR,
                    null
            );
        }
    }


    @Override
    public ApiResponse<LeasedReturnedContainerCountResponse> getLeasedAndReturnedContainersCount(Long restaurantId, Integer productId) {
        try {
            List<Object[]> borrowReturnCountDetails =
                    borrowOrderRepository.getLeasedAndReturnedCounts(restaurantId, productId);

            int leasedCount = 0;
            int returnedCount = 0;

            if (!CollectionUtils.isEmpty(borrowReturnCountDetails)) {
                Object[] row = borrowReturnCountDetails.get(0);
                leasedCount = ((Number) row[0]).intValue();
                returnedCount = ((Number) row[1]).intValue();
            }

            LeasedReturnedContainerCountResponse response =
                    new LeasedReturnedContainerCountResponse(leasedCount, returnedCount);

            return new ApiResponse<>(
                    "Leased & Returned container counts fetched successfully",
                    OrderServiceConstant.STATUS_SUCCESS,
                    response
            );

        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse<>("Failed to fetch leased and returned container counts",
                    OrderServiceConstant.STATUS_ERROR, null);
        }
    }

    @Override
    public ApiResponse<List<LeasedReturnedMonthYearResponse>> getLeasedReturnedMonthYearDetails(LeasedReturnedGraphInput leasedReturnedGraphInput) {
        try {
            List<LeasedReturnedResponse> leasedReturnedResponses;
            if (OrderEnumType.LEASED.equals(leasedReturnedGraphInput.getType())) {
                leasedReturnedResponses = borrowOrderRepository.getLeasedMonthYearDetails(leasedReturnedGraphInput.getRestaurantId(), leasedReturnedGraphInput.getProductId());
            } else if (OrderEnumType.RETURNED.equals(leasedReturnedGraphInput.getType())) {
                leasedReturnedResponses = returnOrderRepository.getReturnedMonthYearDetails(leasedReturnedGraphInput.getRestaurantId(), leasedReturnedGraphInput.getProductId());
            } else {
                return new ApiResponse<>(OrderServiceConstant.STATUS_ERROR, "Invalid type", null);
            }

            Map<String, List<LeasedReturnedResponse>> leasedReturnedGroupedResponse =
                    leasedReturnedResponses.stream()
                            .collect(Collectors.groupingBy(
                                    LeasedReturnedResponse::getMonthYear,
                                    LinkedHashMap::new,
                                    Collectors.toList()
                            ));

            System.err.println("Grouped Response: " + leasedReturnedGroupedResponse);
            log.error("restaurantId={}, productId={}",
                    leasedReturnedGraphInput.getRestaurantId(),
                    leasedReturnedGraphInput.getProductId()
            );

            List<LeasedReturnedMonthYearResponse> response =
                    leasedReturnedGroupedResponse.entrySet().stream().map(entry -> {

                        List<DateWiseReturnCountResponse> dateResponses =
                                entry.getValue().stream()
                                        .map(f -> new DateWiseReturnCountResponse(
                                                f.getDate(),
                                                f.getCount().intValue()
                                        ))
                                        .toList();

                        int total = dateResponses.stream()
                                .mapToInt(DateWiseReturnCountResponse::getLeasedReturnedCount)
                                .sum();

                        return new LeasedReturnedMonthYearResponse(
                                entry.getKey(),
                                total,
                                dateResponses
                        );
                    }).toList();

            return new ApiResponse<>(OrderServiceConstant.STATUS_SUCCESS, "Leased month-year details fetched successfully", response);
        } catch (Exception e) {
            log.error("Failed to fetch leased/returned month-year details for restaurantId={}, productId={}, date={}, type={}", leasedReturnedGraphInput.getRestaurantId(), leasedReturnedGraphInput.getProductId(),
                    leasedReturnedGraphInput.getType(), e);
            return new ApiResponse<>(OrderServiceConstant.STATUS_ERROR, "Failed to fetch leased/returned month-year details", null);
        }
    }

    @Override
    public ApiResponse<List<LeasedReturnedCountWithTimeGraphResponse>> getLeasedReturnedCountWithTimeGraph(LeasedReturnedGraphInput leasedReturnedGraphInput) {

        try {
            LocalDateTime startTime = LocalDate.parse(leasedReturnedGraphInput.getDate(), DateTimeFormatter.ofPattern("dd.MM.yyyy")).atStartOfDay();
            LocalDateTime endTime = startTime.plusDays(1).minusSeconds(1);

            List<LeasedReturnedCountWithTimeGraphProjection> projectionList;

            if (OrderEnumType.LEASED.equals(leasedReturnedGraphInput.getType())) {
                projectionList = borrowOrderRepository.getLeasedCountWithTimeGraph(
                        leasedReturnedGraphInput.getRestaurantId(), leasedReturnedGraphInput.getProductId(), startTime, endTime
                );
            } else if (OrderEnumType.RETURNED.equals(leasedReturnedGraphInput.getType())) {
                projectionList = returnOrderRepository.getReturnedCountWithTimeGraph(
                        leasedReturnedGraphInput.getRestaurantId(), leasedReturnedGraphInput.getProductId(), startTime, endTime
                );
            } else {
                return new ApiResponse<>(OrderServiceConstant.STATUS_ERROR, "Invalid type", null);
            }

            // Map projection to DTO
            List<LeasedReturnedCountWithTimeGraphResponse> response = projectionList.stream()
                    .map(p -> new LeasedReturnedCountWithTimeGraphResponse(
                            p.getLeasedReturnedCount(),
                            p.getTime()
                    ))
                    .toList();

            return new ApiResponse<>(OrderServiceConstant.STATUS_SUCCESS, "Leased/returned count with time graph fetched successfully",
                    response);

        } catch (Exception e) {
            log.error("Failed to fetch leased/returned count with time graph for restaurantId={}, productId={}, date={}, type={}", leasedReturnedGraphInput.getRestaurantId(), leasedReturnedGraphInput.getProductId(),
                    leasedReturnedGraphInput.getDate(), leasedReturnedGraphInput.getType(), e);
            return new ApiResponse<>(
                    OrderServiceConstant.STATUS_ERROR,
                    "Failed to fetch leased/returned count with time graph",
                    null
            );
        }
    }


    @Override
    public ApiResponse<MostAndLeastUsedContainerResponse> getMostAndLeastUsedContainer(Long restaurantId) {
        try {
            // 1. Borrow usage from order DB
            List<Object[]> usageList =
                    borrowOrderRepository.findUsageByRestaurant(restaurantId);

            if (usageList == null || usageList.isEmpty()) {
                return new ApiResponse<>(
                        OrderServiceConstant.STATUS_SUCCESS,
                        "No borrow data found",
                        new MostAndLeastUsedContainerResponse(List.of(), List.of())
                );
            }

            // 2. Inventory from Inventory service
            ApiResponse<List<RestaurantContainerInventoryResponse>> inventoryResp =
                    inventoryFeignClient.getRestaurantContainerInventoryByRestaurantId(restaurantId);

            List<RestaurantContainerInventoryResponse> inventoryList =
                    inventoryResp.getData();

            if (inventoryList == null || inventoryList.isEmpty()) {
                return new ApiResponse<>(
                        OrderServiceConstant.STATUS_SUCCESS,
                        "No inventory data found",
                        new MostAndLeastUsedContainerResponse(List.of(), List.of())
                );
            }

            // 3. Map inventory by productUniqueId
            Map<Integer, RestaurantContainerInventoryResponse> inventoryMap =
                    inventoryList.stream().collect(
                            Collectors.toMap(
                                    RestaurantContainerInventoryResponse::getContainerTypeId,
                                    i -> i
                            )
                    );

            // 4. Calculate percentage for each product
            List<ProductUsageTemp> tempList = new ArrayList<>();

            for (Object[] obj : usageList) {
                Long productId = ((Number) obj[0]).longValue();
                long borrowCount = ((Number) obj[1]).longValue();

                RestaurantContainerInventoryResponse inv =
                        inventoryMap.get(productId.intValue());

                if (inv == null || inv.getCurrentQuantity() == null
                        || inv.getCurrentQuantity() == 0) continue;

                double percentage =
                        round((borrowCount * 100.0) / inv.getCurrentQuantity());

                tempList.add(new ProductUsageTemp(inv, percentage));
            }

            if (tempList.isEmpty()) {
                return new ApiResponse<>(
                        OrderServiceConstant.STATUS_SUCCESS,
                        "No matching inventory found",
                        new MostAndLeastUsedContainerResponse(List.of(), List.of())
                );
            }

            // 5. Find MAX and MIN percentages
            double maxPercentage = tempList.stream()
                    .mapToDouble(ProductUsageTemp::getPercentage)
                    .max()
                    .orElse(0);

            double minPercentage = tempList.stream()
                    .mapToDouble(ProductUsageTemp::getPercentage)
                    .min()
                    .orElse(0);

            List<MostUsedContainerResponse> mostUsedList = new ArrayList<>();
            List<LeastUsedContainerResponse> leastUsedList = new ArrayList<>();

            // 6. Collect ALL ties
            for (ProductUsageTemp temp : tempList) {
                double percentage = temp.getPercentage();
                RestaurantContainerInventoryResponse inv = temp.getInventory();

                if (Double.compare(percentage, maxPercentage) == 0) {
                    mostUsedList.add(new MostUsedContainerResponse(
                            inv.getContainerTypeId(),
                            percentage,
                            inv.getContainerTypeName(),
                            inv.getProductUniqueId(),
                            inv.getCapacity(),
                            inv.getProductImageUrl()
                    ));
                }

                if (Double.compare(percentage, minPercentage) == 0) {
                    leastUsedList.add(new LeastUsedContainerResponse(
                            inv.getContainerTypeId(),
                            percentage,
                            inv.getContainerTypeName(),
                            inv.getProductUniqueId(),
                            inv.getCapacity(),
                            inv.getProductImageUrl()
                    ));
                }
            }

            return new ApiResponse<>(
                    OrderServiceConstant.STATUS_SUCCESS,
                    "Fetched successfully",
                    new MostAndLeastUsedContainerResponse(
                            mostUsedList,
                            leastUsedList
                    )
            );

        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse<>(
                    OrderServiceConstant.STATUS_ERROR,
                    "Failed to fetch most and least used container",
                    null
            );
        }
    }

    @Override
    public ApiResponse<List<BorrowOrder>> getBorrowedOrderByOrderId(Long orderId) {
        try {
            List<BorrowOrder> borrowOrders = borrowOrderRepository.findByOrderId(orderId);
            if (CollectionUtils.isEmpty(borrowOrders)) {
                return new ApiResponse<>(OrderServiceConstant.STATUS_SUCCESS, "No borrowed orders found", null);
            }
            return new  ApiResponse<>(OrderServiceConstant.STATUS_SUCCESS, "Borrowed details fetched successfully", borrowOrders);
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse<>(
                    OrderServiceConstant.STATUS_ERROR,
                    "Failed to fetch borrowed details",
                    null
            );
        }
    }


    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }




    private Map<String, Object> handleReturnError(Exception ex) {

        return ApiResponseUtil.error(
                ex.getMessage() != null
                        ? ex.getMessage()
                        : "Failed to return containers"
        );
    }

    @Override
    public Map<String, Object> getMonthWiseOrders(Long userId) {
        // 'year' is ignored, kept only for compatibility

        Map<String, Object> response = new HashMap<>();

        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime twoMonthsAgo = now.minusMonths(3);

            // 1️⃣ Fetch last 2 months
            List<BorrowOrder> borrowOrders =
                    borrowOrderRepository.findAllByUserIdBetweenDates(
                            userId, twoMonthsAgo, now
                    );

            // 2️⃣ Month map (Current → Previous)
            Map<String, List<OrderListDetails>> monthWiseOrders = new LinkedHashMap<>();

            for (int i = 0; i < 3; i++) {
                LocalDateTime dt = now.minusMonths(i);
                String monthName =
                        dt.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
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
                List<ProductResponse> products = inventoryFeignClient.getProductsByIds(productIds).getData();
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
                                        p != null ? p.getProductUniqueId() : null
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
                            .restaurantAddress(restaurant != null ? restaurant.getName() : null)
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
            response.put("message", "Last 2 months orders fetched successfully");
            response.put("value", monthWiseOrders);
            return response;
        }catch (Exception ex) {
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
                    inventoryFeignClient.getProductsByIds(productIds).getData();

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
        } catch (Exception ex) {

            response.put("status", "error");
            response.put("message", "Failed to fetch order details");
            response.put("data", null);
            return response;
        }
    }

    @Transactional(rollbackOn = Exception.class)
    @Override
    public Map<String, Object> approveOrder(Long orderId) {

        try {

            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Order not found with id: " + orderId));

            // Already approved
            if (OrderServiceConstant.APPROVED
                    .equalsIgnoreCase(order.getOrderStatus())) {
                throw new RuntimeException("Order is already APPROVED");
            }

            // Only pending allowed
            if (!OrderServiceConstant.PENDING
                    .equalsIgnoreCase(order.getOrderStatus())) {
                throw new RuntimeException(
                        "Only PENDING orders can be approved");
            }

            List<BorrowOrder> borrowOrders =
                    borrowOrderRepository
                            .findByOrderId(orderId);

            if (borrowOrders.isEmpty()) {
                throw new RuntimeException(
                        "No borrow items found");
            }

            // Build qty map
            Map<Integer, Integer> qtyMap =
                    borrowOrders.stream()
                            .collect(Collectors.groupingBy(
                                    b -> b.getProductId().intValue(),
                                    Collectors.summingInt(
                                            BorrowOrder::getQuantity)
                            ));

            ReduceInventoryRequest request =
                    new ReduceInventoryRequest();
            request.setRestaurantId(
                    borrowOrders.get(0)
                            .getRestaurantId());
            request.setContainerQtyMap(qtyMap);

            // 🔥 Call inventory
            ApiResponse<?> feignResponse =
                    inventoryFeignClient
                            .reduceAvailableContainers(request);

            if (OrderServiceConstant.STATUS_SUCCESS
                    .equalsIgnoreCase(
                            feignResponse.getStatus())) {

                order.setOrderStatus(
                        OrderServiceConstant.APPROVED);
                orderRepository.save(order);

                return Map.of(
                        OrderServiceConstant.STATUS,
                        OrderServiceConstant.STATUS_SUCCESS,
                        OrderServiceConstant.MESSAGE,
                        "Order status updated to APPROVED"
                );
            }

            // Inventory rejected
            order.setOrderStatus(
                    OrderServiceConstant.REJECTED);
            orderRepository.save(order);

            System.err.println(feignResponse.getMessage());

            return Map.of(
                    OrderServiceConstant.STATUS,
                    OrderServiceConstant.STATUS_ERROR,
                    OrderServiceConstant.MESSAGE,
                    feignResponse.getMessage()
            );

        }catch (Exception ex) {

            // Any business failure → rollback
            throw new RuntimeException(
                    ex.getMessage() != null
                            ? ex.getMessage()
                            : "Failed to approve order");
        }
    }



    @Override
    public Map<String, Object> getMonthWiseReturnOrders(Long userId) {

        Map<String, Object> response = new HashMap<>();

        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime twoMonthsAgo = now.minusMonths(2);

            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");

            // 1️⃣ Fetch last 2 months returns
            List<ReturnOrder> returnOrders =
                    returnOrderRepository.findAllByUserIdBetweenDates(
                            userId, twoMonthsAgo, now
                    );

            // 2️⃣ Month map (Current → Previous)
            Map<String, List<OrderListDetails>> monthWiseReturns = new LinkedHashMap<>();
            for (int i = 0; i < 2; i++) {
                LocalDateTime dt = now.minusMonths(i);
                String monthName =
                        dt.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
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
                        inventoryFeignClient.getProductsByIds(productIds).getData();

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
                    Long borrowOrderId = first.getBorrowOrderId();
                    BorrowOrder borrowOrder = borrowOrderRepository.findById(borrowOrderId)
                            .orElseThrow(() -> new ResourceNotFoundException("Borrow order not found with id: " + borrowOrderId));
                    String monthName = Month.of(dt.getMonthValue())
                            .getDisplayName(TextStyle.FULL, Locale.ENGLISH);

                    OrderListDetails details = OrderListDetails.builder()
                            .orderId(first.getBorrowOrderId())
                            .restaurantId(first.getRestaurantId())
                            .restaurantName(restaurant != null ? restaurant.getName() : null)
                            .restaurantAddress(restaurant != null ? restaurant.getAddress() : null)
                            .productCount(productList.size())
                            .totalContainerCount(totalReturned)
                            .orderDate(borrowOrder.getBorrowedAt().toLocalDate().toString())
                            .orderTime(borrowOrder.getBorrowedAt().toLocalTime().format(timeFormatter)) // ⭐ AM/PM format
                            .returnedDate(dt.toLocalDate().toString())
                            .returnedTime(dt.toLocalTime().format(timeFormatter))
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
    public ApiResponse<List<ProductDetailsResponse>> getBorrowedProductSummary(Long userId, String customerId) {

        try {
            List<BorrowOrderResponse> rows = new ArrayList<>();

            if (userId != null){
                rows = borrowOrderRepository.getProductBorrowReturnSummary(userId);
            }
            if (customerId != null){
                ApiResponse<UserResponse> userResponse = authClient.getUserByCustomerId(customerId);
                if (userResponse == null || userResponse.getData() == null) {
                    return new ApiResponse<>("error",
                            "User not found for customerId: " + customerId,
                            null);
                }
                userId = userResponse.getData().getId();
                rows = borrowOrderRepository.getProductBorrowReturnSummary(userId);
            }
            if (rows == null || rows.isEmpty()) {
                return new ApiResponse<>("success", "No borrowed products found for user", Collections.emptyList());
            }
            Set<Long> productIds = rows.stream()
                    .map(BorrowOrderResponse::getProductId)   // use getter
                    .collect(Collectors.toSet());

            Map<Long, ProductResponse> productMap = new HashMap<>();
            try {
                List<ProductResponse> products =
                        inventoryFeignClient.getProductsByIds(productIds.stream().map(Long::intValue).toList()).getData();


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

            Map<Long, ProductResponse> finalProductMap = productMap;
            Long finalUserId = userId;
            List<ProductDetailsResponse> result = rows.stream().map(r -> {
                int remainingQty = Math.max(0, r.getRemainingQty());
                long daysPassed = ChronoUnit.DAYS.between(r.getOrderDate(), LocalDate.now());
                long daysLeft = Math.max(0, 7 - daysPassed);
                ProductResponse p = finalProductMap.get(r.getProductId());
                return new ProductDetailsResponse(
                        finalUserId,
                        r.getOrderId(),
                        r.getProductId(),
                        p != null ? p.getProductName() : null,
                        remainingQty,
                        p != null ? p.getProductImageUrl() : null,
                        daysLeft,
                        p != null ? p.getProductUniqueId() : null,
                        p != null ? p.getCapacity() : null,
                        r.getDueDate());
            }).collect(Collectors.toList());

            return new ApiResponse<>("success", "Borrowed product summary fetched successfully", result);

        } catch (Exception ex) {
            log.error("Error occurred while fetching borrowed order response {}", ex.getMessage());
            return new ApiResponse<>("error", "Failed to fetch borrowed product summary", null);

        }
    }
    @Override
    public ContainerChartResponse getChartStatistics(Long restaurantId, Integer month, Integer year, Long productId, Integer planId) {

        LocalDateTime startDate = LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime endDate = LocalDateTime.now().plusYears(10);
        String monthYearStr = "All Time";

        if (month != null && year != null) {
            startDate = LocalDateTime.of(year, month, 1, 0, 0);
            endDate = startDate.plusMonths(1).minusSeconds(1);
            monthYearStr = startDate.format(DateTimeFormatter.ofPattern("MMMM-yyyy"));
        }

        Integer leaseCount = borrowOrderRepository.getTotalLeased(restaurantId, productId, startDate, endDate);
        Integer receiveCount = returnOrderRepository.getTotalReturned(restaurantId, productId, startDate, endDate);

        Integer containerTypeId = (productId != null) ? productId.intValue() : null;
        Integer trueTotal = 0;
        Integer trueAvailable = 0;
        Integer trueDamage = 0;

        try {
            // 🟢 FIX: Call the new Feign client method name
            TrueInventoryStatsDto stats = inventoryFeignClient.getContainerStats(restaurantId, containerTypeId, month, year);
            if (stats != null) {
                trueTotal = stats.getTotal() != null ? stats.getTotal() : 0;
                trueAvailable = stats.getAvailable() != null ? stats.getAvailable() : 0;
                // 🟢 FIX: Use the new getter name
                trueDamage = stats.getDamageCount() != null ? stats.getDamageCount() : 0;
            }
        } catch (Exception e) {
            log.error("Failed to fetch true inventory stats from Inventory Service: {}", e.getMessage());
        }

        return ContainerChartResponse.builder()
                .total(trueTotal)
                .lease(leaseCount)
                .receive(receiveCount)
                .damage(trueDamage)
                .available(trueAvailable)
                .monthYear(monthYearStr)
                .build();
    }

    @Override
    public void markAsSold(SoldRequest request) {

    }

    private Integer getTotalContainers(Integer planId) {
        try {
            ResponseEntity<Map<String, Object>> response =
                    inventoryFeignClient.getSubscriptionPlanById(planId);

            Map<String, Object> body = response.getBody();

            if (body != null && "success".equals(body.get("status"))) {
                Map<String, Object> data = (Map<String, Object>) body.get("data");

                if (data != null && data.get("totalContainers") != null) {
                    return ((Number) data.get("totalContainers")).intValue();
                }
            }

        } catch (Exception e) {
            log.error("Error fetching subscription plan: {}", e.getMessage());
        }

        return 0; // fallback
    }

}
