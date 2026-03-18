package com.notification.service;

import com.notification.dto.NotificationResponseDto;
import com.notification.entity.NotificationDetails;
import com.notification.repository.NotificationRepository;
import lombok.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    // ── Common: Calculate 1 month ago from today ──────────
    private LocalDateTime getOneMonthAgo() {
        return LocalDateTime.now().minusMonths(1);
    }

    // ── Common: Parse String timestamp & check last month ─
    private boolean isWithinLastMonth(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) {
            return false;
        }
        try {
            LocalDateTime notifTime =
                    LocalDateTime.parse(timestamp);
            return notifTime.isAfter(getOneMonthAgo());
        } catch (Exception e) {
            return false; // skip invalid timestamps
        }
    }

    // ── 1. Get ALL notifications (read + unread) ──────────
    // last 1 month only
    public List<NotificationResponseDto> getAllNotifications(
            Long receiverId) {

        return notificationRepository
                .findAllByReceiverId(receiverId)
                .stream()
                .filter(n -> isWithinLastMonth(n.getTimestamp()))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    // ── 2. Get only UNREAD notifications ──────────────────
    // last 1 month only
    public List<NotificationResponseDto> getUnreadNotifications(
            Long receiverId) {

        return notificationRepository
                .findUnreadByReceiverId(receiverId)
                .stream()
                .filter(n -> isWithinLastMonth(n.getTimestamp()))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    // ── 3. Get only READ notifications ────────────────────
    // last 1 month only
    public List<NotificationResponseDto> getReadNotifications(
            Long receiverId) {

        return notificationRepository
                .findReadByReceiverId(receiverId)
                .stream()
                .filter(n -> isWithinLastMonth(n.getTimestamp()))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    // ── 4. Get unread COUNT ───────────────────────────────
    // last 1 month only
    public Long getUnreadCount(Long receiverId) {

        return notificationRepository
                .findUnreadByReceiverId(receiverId)
                .stream()
                .filter(n -> isWithinLastMonth(n.getTimestamp()))
                .count();
    }

    // ── 5. Get notifications by TYPE ──────────────────────
    // last 1 month only
    public List<NotificationResponseDto> getByType(
            Long receiverId, String type) {

        return notificationRepository
                .findByReceiverIdAndType(receiverId, type)
                .stream()
                .filter(n -> isWithinLastMonth(n.getTimestamp()))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    // ── 6. Mark ONE notification as READ ──────────────────
    @Transactional
    public String markAsRead(Long id, Long receiverId) {

        int updated = notificationRepository
                .markAsRead(id, receiverId);

        if (updated == 0) {
            throw new RuntimeException(
                    "Notification not found with id: " + id);
        }
        return "Notification marked as read";
    }

    // ── 7. Mark ALL notifications as READ ─────────────────
    @Transactional
    public String markAllAsRead(Long receiverId) {

        int updated = notificationRepository
                .markAllAsRead(receiverId);

        return updated + " notifications marked as read";
    }

    // ── 8. Get summary (total + unread count) ─────────────
    // last 1 month only
    public NotificationSummary getNotificationSummary(
            Long receiverId) {

        // Fetch all + filter last 1 month
        List<NotificationDetails> lastMonthData =
                notificationRepository
                        .findAllByReceiverId(receiverId)
                        .stream()
                        .filter(n -> isWithinLastMonth(n.getTimestamp()))
                        .collect(Collectors.toList());

        long unreadCount = lastMonthData.stream()
                .filter(n -> Boolean.FALSE.equals(n.getIsRead()))
                .count();

        long readCount = lastMonthData.stream()
                .filter(n -> Boolean.TRUE.equals(n.getIsRead()))
                .count();

        return NotificationSummary.builder()
                .totalCount((long) lastMonthData.size())
                .unreadCount(unreadCount)
                .readCount(readCount)
                .fromDate(getOneMonthAgo()
                        .toString())         // show from date
                .toDate(LocalDateTime.now()
                        .toString())         // show to date
                .notifications(lastMonthData.stream()
                        .map(this::mapToDto)
                        .collect(Collectors.toList()))
                .build();
    }

    // ── Mapper: Entity → DTO ──────────────────────────────
    private NotificationResponseDto mapToDto(
            NotificationDetails n) {

        return NotificationResponseDto.builder()
                .id(n.getId())
                .message(n.getMessage())
                .notificationType(n.getNotificationType())
                .senderId(n.getSenderId())
                .receiverId(n.getReceiverId())
                .timestamp(n.getTimestamp())
                .isRead(n.getIsRead())
                .approvalStatus(n.getApprovalStatus())
                .orderId(n.getOrderId())
                .status(n.getStatus())
                .build();
    }

    // ── Inner summary DTO ─────────────────────────────────
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationSummary {
        private Long totalCount;
        private Long unreadCount;
        private Long readCount;
        private String fromDate;     // 1 month ago date
        private String toDate;       // today date
        private List<NotificationResponseDto> notifications;
    }
}