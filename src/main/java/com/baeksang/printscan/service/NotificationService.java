package com.baeksang.printscan.service;

import com.baeksang.printscan.entity.Notification;
import com.baeksang.printscan.entity.User;
import com.baeksang.printscan.repository.NotificationRepository;
import com.baeksang.printscan.repository.UserRepository;
import lombok.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional
    public Notification createNotification(User user, String title, String message, String type) {
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(type)
                .build();

        return notificationRepository.save(notification);
    }

    @Transactional
    public void sendNotification(User user, String title, String message) {
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다."));
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    public Page<Notification> getNotifications(User user, Pageable pageable) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user, pageable);
    }

    public long countUnreadNotifications(User user) {
        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    @Transactional
    public void markAllAsRead(User user) {
        notificationRepository.markAllAsRead(user);
    }

    @Transactional
    public void deleteOldNotifications(User user) {
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        notificationRepository.deleteOldNotifications(user, oneMonthAgo);
    }

    @Transactional(readOnly = true)
    public Page<Notification> getNotifications(String username, Pageable pageable) {
        return notificationRepository.findByUserUsernameOrderByCreatedAtDesc(username, pageable);
    }

    @Transactional
    public void markAsRead(Long notificationId, String username) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다."));

        if (!notification.getUser().getUsername().equals(username)) {
            throw new IllegalArgumentException("해당 알림에 접근할 권한이 없습니다.");
        }

        notification.setRead(true);
    }

    public long getUnreadCount(User user) {
        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    @Scheduled(cron = "0 0 0 * * *") // 매일 자정에 실행
    public void cleanupOldNotifications() {
        List<User> users = userRepository.findAll();
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        
        for (User user : users) {
            notificationRepository.deleteOldNotifications(user, oneMonthAgo);
        }
    }
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class NotificationMessage {
    private String title;
    private String message;
    private long timestamp;
} 