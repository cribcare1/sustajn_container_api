package com.notification.controller;

import com.notification.constant.NotificationConstant;
import com.notification.dto.ApiResponse;
import com.notification.dto.NotificationResponseDto;
import com.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notificationsDetails")
@RequiredArgsConstructor
public class NotificationDetailsController {

    private final NotificationService notificationService;

    // ── 1. Get ALL notifications (read + unread) ──────────
    @GetMapping("/all/{receiverId}")
    public ResponseEntity<ApiResponse<List<NotificationResponseDto>>> getAll(
            @PathVariable Long receiverId) {

        if (receiverId == null || receiverId <= 0) {
            return ResponseEntity
                    .badRequest()
                    .body(new ApiResponse<>(
                            NotificationConstant.ERROR,
                            "Invalid receiverId."
                                    + " Must be a positive number"));
        }

        List<NotificationResponseDto> data =
                notificationService.getAllNotifications(receiverId);

        if (data.isEmpty()) {
            return ResponseEntity.ok(
                    new ApiResponse<>(
                            NotificationConstant.SUCCESS,
                            "No notifications found"
                                    + " for receiverId: " + receiverId,
                            data));
        }

        return ResponseEntity.ok(
                new ApiResponse<>(
                                        NotificationConstant.SUCCESS,
                        data.size()
                                + " notifications fetched successfully",
                        data));
    }

    // ── 2. Get only UNREAD notifications ──────────────────
    @GetMapping("/unread/{receiverId}")
    public ResponseEntity<ApiResponse<List<NotificationResponseDto>>> getUnread(
            @PathVariable Long receiverId) {

        if (receiverId == null || receiverId <= 0) {
            return ResponseEntity
                    .badRequest()
                    .body(new ApiResponse<>(
                            NotificationConstant.ERROR,
                            "Invalid receiverId."
                                    + " Must be a positive number"));
        }

        List<NotificationResponseDto> data =
                notificationService.getUnreadNotifications(receiverId);

        if (data.isEmpty()) {
            return ResponseEntity.ok(
                    new ApiResponse<>(
                            NotificationConstant.SUCCESS,
                            "No unread notifications found"
                                    + " for receiverId: " + receiverId,
                            data));
        }

        return ResponseEntity.ok(
                new ApiResponse<>(
                        NotificationConstant.SUCCESS,
                        data.size()
                                + " unread notifications fetched successfully",
                        data));
    }

    // ── 3. Get only READ notifications ────────────────────
    @GetMapping("/read/{receiverId}")
    public ResponseEntity<ApiResponse<List<NotificationResponseDto>>> getRead(
            @PathVariable Long receiverId) {

        if (receiverId == null || receiverId <= 0) {
            return ResponseEntity
                    .badRequest()
                    .body(new ApiResponse<>(
                            NotificationConstant.ERROR,
                            "Invalid receiverId."
                                    + " Must be a positive number"));
        }

        List<NotificationResponseDto> data =
                notificationService.getReadNotifications(receiverId);

        if (data.isEmpty()) {
            return ResponseEntity.ok(
                    new ApiResponse<>(
                                            NotificationConstant.SUCCESS,
                            "No read notifications found"
                                    + " for receiverId: " + receiverId,
                            data));
        }

        return ResponseEntity.ok(
                new ApiResponse<>(
                                        NotificationConstant.SUCCESS,
                        data.size()
                                + " read notifications fetched successfully",
                        data));
    }

    // ── 4. Get UNREAD COUNT (bell badge 🔔) ───────────────
    @GetMapping("/unread/count/{receiverId}")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            @PathVariable Long receiverId) {

        if (receiverId == null || receiverId <= 0) {
            return ResponseEntity
                    .badRequest()
                    .body(new ApiResponse<>(
                            NotificationConstant.ERROR,
                            "Invalid receiverId."
                                    + " Must be a positive number"));
        }

        Long count = notificationService
                .getUnreadCount(receiverId);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        NotificationConstant.SUCCESS,
                        "Unread notification count"
                                + " fetched successfully",
                        count));
    }

    // ── 5. Get notifications by TYPE ──────────────────────
    @GetMapping("/type/{receiverId}")
    public ResponseEntity<ApiResponse<List<NotificationResponseDto>>> getByType(
            @PathVariable Long receiverId,
            @RequestParam String type) {

        if (receiverId == null || receiverId <= 0) {
            return ResponseEntity
                    .badRequest()
                    .body(new ApiResponse<>(
                            NotificationConstant.ERROR,
                            "Invalid receiverId."
                                    + " Must be a positive number"));
        }

        if (type == null || type.isBlank()) {
            return ResponseEntity
                    .badRequest()
                    .body(new ApiResponse<>(
                            NotificationConstant.ERROR,
                            "Notification type"
                                    + " cannot be empty"));
        }

        List<NotificationResponseDto> data =
                notificationService.getByType(receiverId, type);

        if (data.isEmpty()) {
            return ResponseEntity.ok(
                    new ApiResponse<>(
                            NotificationConstant.SUCCESS,
                            "No notifications found"
                                    + " for type: " + type,
                            data));
        }

        return ResponseEntity.ok(
                new ApiResponse<>(
                        NotificationConstant.SUCCESS,
                        data.size()
                                + " notifications fetched"
                                + " for type: " + type,
                        data));
    }

    // ── 6. Mark ONE notification as READ ──────────────────
    @PutMapping("/mark-read/{id}")
    public ResponseEntity<ApiResponse<String>> markAsRead(
            @PathVariable Long id,
            @RequestParam Long receiverId) {

        if (id == null || id <= 0) {
            return ResponseEntity
                    .badRequest()
                    .body(new ApiResponse<>(
                            NotificationConstant.ERROR,
                            "Invalid notification id."
                                    + " Must be a positive number"));
        }

        if (receiverId == null || receiverId <= 0) {
            return ResponseEntity
                    .badRequest()
                    .body(new ApiResponse<>(
                            NotificationConstant.ERROR,
                            "Invalid receiverId."
                                    + " Must be a positive number"));
        }

        try {
            String result = notificationService
                    .markAsRead(id, receiverId);

            return ResponseEntity.ok(
                    new ApiResponse<>(
                            NotificationConstant.SUCCESS,
                            result));

        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(
                            NotificationConstant.ERROR,
                            e.getMessage()));
        }
    }

    // ── 7. Mark ALL notifications as READ ─────────────────
    @PutMapping("/mark-all-read/{receiverId}")
    public ResponseEntity<ApiResponse<String>> markAllAsRead(
            @PathVariable Long receiverId) {

        if (receiverId == null || receiverId <= 0) {
            return ResponseEntity
                    .badRequest()
                    .body(new ApiResponse<>(
                            NotificationConstant.ERROR,
                            "Invalid receiverId."
                                    + " Must be a positive number"));
        }

        String result = notificationService
                .markAllAsRead(receiverId);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        NotificationConstant.SUCCESS,
                        result));
    }
}