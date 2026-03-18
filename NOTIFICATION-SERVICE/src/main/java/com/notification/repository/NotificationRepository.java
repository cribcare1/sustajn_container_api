package com.notification.repository;


import com.notification.entity.NotificationDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface NotificationRepository
        extends JpaRepository<NotificationDetails, Long> {

    // ── Fetch all notifications for a receiver ───────────
    // with read/unread status
    @Query("""
        SELECT n FROM NotificationDetails n
        WHERE n.receiverId = :receiverId
        AND n.status = 'ACTIVE'
        ORDER BY n.timestamp DESC
        """)
    List<NotificationDetails> findAllByReceiverId(
            @Param("receiverId") Long receiverId);

    // ── Fetch only UNREAD notifications ──────────────────
    @Query("""
        SELECT n FROM NotificationDetails n
        WHERE n.receiverId = :receiverId
        AND n.isRead = false
        AND n.status = 'ACTIVE'
        ORDER BY n.timestamp DESC
        """)
    List<NotificationDetails> findUnreadByReceiverId(
            @Param("receiverId") Long receiverId);

    // ── Fetch only READ notifications ─────────────────────
    @Query("""
        SELECT n FROM NotificationDetails n
        WHERE n.receiverId = :receiverId
        AND n.isRead = true
        AND n.status = 'ACTIVE'
        ORDER BY n.timestamp DESC
        """)
    List<NotificationDetails> findReadByReceiverId(
            @Param("receiverId") Long receiverId);

    // ── Count unread notifications ────────────────────────
    @Query("""
        SELECT COUNT(n) FROM NotificationDetails n
        WHERE n.receiverId = :receiverId
        AND n.isRead = false
        AND n.status = 'ACTIVE'
        """)
    Long countUnreadByReceiverId(
            @Param("receiverId") Long receiverId);

    // ── Fetch by notification type ────────────────────────
    @Query("""
        SELECT n FROM NotificationDetails n
        WHERE n.receiverId = :receiverId
        AND n.notificationType = :type
        AND n.status = 'ACTIVE'
        ORDER BY n.timestamp DESC
        """)
    List<NotificationDetails> findByReceiverIdAndType(
            @Param("receiverId") Long receiverId,
            @Param("type") String type);

    // ── Mark single notification as read ──────────────────
    @Modifying
    @Query("""
        UPDATE NotificationDetails n
        SET n.isRead = true
        WHERE n.id = :id
        AND n.receiverId = :receiverId
        """)
    int markAsRead(
            @Param("id") Long id,
            @Param("receiverId") Long receiverId);

    // ── Mark ALL notifications as read for a user ─────────
    @Modifying
    @Query("""
        UPDATE NotificationDetails n
        SET n.isRead = true
        WHERE n.receiverId = :receiverId
        AND n.isRead = false
        """)
    int markAllAsRead(
            @Param("receiverId") Long receiverId);
}