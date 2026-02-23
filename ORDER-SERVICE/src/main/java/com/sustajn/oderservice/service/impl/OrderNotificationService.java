package com.sustajn.oderservice.service.impl;

import com.sustajn.oderservice.constant.NotificationStatus;
import com.sustajn.oderservice.entity.BorrowOrder;
import com.sustajn.oderservice.entity.Notification;
import com.sustajn.oderservice.entity.Order;
import com.sustajn.oderservice.repository.BorrowOrderRepository;
import com.sustajn.oderservice.repository.NotificationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderNotificationService {

    private final BorrowOrderRepository borrowOrderRepository;
    private final NotificationRepository notificationRepository;


    @Scheduled(cron = "0 0 1 * * ?")
//    @Scheduled(cron = "* * * * * ?")
    @Transactional
    public void sendBorrowOrderNotificationsNEW() {

        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();

        List<BorrowOrder> borrowOrders =
                borrowOrderRepository.findActiveBorrowOrders();

        // 🔹 Group by orderId
        Map<Long, List<BorrowOrder>> ordersMap =
                borrowOrders.stream()
                        .collect(Collectors.groupingBy(BorrowOrder::getOrderId));

        for (Map.Entry<Long, List<BorrowOrder>> entry : ordersMap.entrySet()) {

            Long orderId = entry.getKey();
            List<BorrowOrder> orderBorrowOrders = entry.getValue();

            // ❌ Skip order if all items are returned
            boolean allReturned = orderBorrowOrders.stream()
                    .allMatch(bo -> bo.getReturnedQuantity() >= bo.getQuantity());

            if (allReturned) {
                continue;
            }

            boolean shouldNotify = false;
            boolean isExtended = false;

            for (BorrowOrder borrowOrder : orderBorrowOrders) {

                LocalDate dueDate = borrowOrder.getIsExtended()
                        ? borrowOrder.getEffectiveDueDate().toLocalDate()
                        : borrowOrder.getDueDate().toLocalDate();

                boolean inLast3Days =
                        !today.isAfter(dueDate) &&
                                !today.isBefore(dueDate.minusDays(3));

                if (inLast3Days) {
                    shouldNotify = true;
                    isExtended = borrowOrder.getIsExtended();
                    break; // ✅ one order → one decision
                }
            }

            if (!shouldNotify) {
                continue;
            }

            NotificationStatus type = isExtended
                    ? NotificationStatus.EXTENDED_BEFORE_3_DAYS
                    : NotificationStatus.BEFORE_3_DAYS;

            boolean alreadySentToday =
                    notificationRepository
                            .existsByOrderIdAndTypeAndSentDate(
                                    orderId,
                                    type,
                                    todayStart
                            );

            if (!alreadySentToday) {
                saveOrderNotification(
                        orderId,
                        type,
                        isExtended
                                ? "Your extended due date is approaching. Please return the containers."
                                : "Your due date is approaching. Please return the containers."
                );
            }
        }
    }
    private void saveOrderNotification(
            Long orderId,
            NotificationStatus type,
            String message
    ) {

        Notification notification = new Notification();
        notification.setOrderId(orderId);

        notification.setType(type);
        notification.setMessage(message);
        notification.setSentDate(LocalDateTime.now());
        notification.setIsRead(false);

        notificationRepository.save(notification);
    }


    @Transactional
    public void extendBorrowOrder(Long orderId) {

        List<BorrowOrder> borrowOrders =
                borrowOrderRepository.findByOrderId(orderId);

        if (borrowOrders.isEmpty()) {
            throw new RuntimeException("No borrow orders found for orderId: " + orderId);
        }

        LocalDateTime now = LocalDateTime.now();
        List<BorrowOrder> toUpdate = new ArrayList<>();

        for (BorrowOrder borrowOrder : borrowOrders) {

            // ❌ Skip fully returned items
            if (borrowOrder.getReturnedQuantity() >= borrowOrder.getQuantity()) {
                continue;
            }

            // ❌ Prevent double extension
            if (Boolean.TRUE.equals(borrowOrder.getIsExtended())) {
                continue;
            }

            // ✅ Base due date
            LocalDateTime baseDueDate =
                    borrowOrder.getEffectiveDueDate() != null
                            ? borrowOrder.getEffectiveDueDate()
                            : borrowOrder.getDueDate();

            borrowOrder.setIsExtended(true);
            borrowOrder.setExtendedAt(now);
            borrowOrder.setEffectiveDueDate(baseDueDate.plusDays(5));

            toUpdate.add(borrowOrder);
        }

        // ✅ SINGLE DB CALL
        if (!toUpdate.isEmpty()) {
            borrowOrderRepository.saveAll(toUpdate);
        }
    }

}
