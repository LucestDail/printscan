package com.baeksang.printscan.service;

import com.baeksang.printscan.entity.User;
import com.baeksang.printscan.entity.UserActivityLog;
import com.baeksang.printscan.repository.UserActivityLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserActivityService {

    private final UserActivityLogRepository activityLogRepository;

    @Transactional
    public void logActivity(User user, String activityType, String description, HttpServletRequest request) {
        UserActivityLog log = UserActivityLog.builder()
                .user(user)
                .activityType(activityType)
                .description(description)
                .ipAddress(getClientIp(request))
                .build();
        
        activityLogRepository.save(log);
    }

    public Page<UserActivityLog> getUserActivities(User user, Pageable pageable) {
        return activityLogRepository.findByUser(user, pageable);
    }

    public Page<UserActivityLog> getUserActivitiesByDateRange(
            User user, LocalDateTime start, LocalDateTime end, Pageable pageable) {
        return activityLogRepository.findByUserAndCreatedAtBetween(user, start, end, pageable);
    }

    public Page<UserActivityLog> getActivitiesByType(String activityType, Pageable pageable) {
        return activityLogRepository.findByActivityType(activityType, pageable);
    }

    private String getClientIp(HttpServletRequest request) {
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getHeader("Proxy-Client-IP");
        }
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getHeader("WL-Proxy-Client-IP");
        }
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getRemoteAddr();
        }
        return clientIp;
    }
} 