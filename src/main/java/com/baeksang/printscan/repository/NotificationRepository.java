package com.baeksang.printscan.repository;

import com.baeksang.printscan.entity.Notification;
import com.baeksang.printscan.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    long countByUserAndIsReadFalse(User user);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user = :user AND n.isRead = false")
    void markAllAsRead(@Param("user") User user);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.user = :user AND n.createdAt < :date")
    void deleteByUserAndCreatedAtBefore(@Param("user") User user, @Param("date") LocalDateTime date);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.user = :user AND n.isRead = true AND n.createdAt < :date")
    void deleteOldNotifications(@Param("user") User user, @Param("date") LocalDateTime date);

    Page<Notification> findByUserUsernameOrderByCreatedAtDesc(String username, Pageable pageable);
} 