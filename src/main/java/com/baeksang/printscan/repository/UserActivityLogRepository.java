package com.baeksang.printscan.repository;

import com.baeksang.printscan.entity.User;
import com.baeksang.printscan.entity.UserActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface UserActivityLogRepository extends JpaRepository<UserActivityLog, Long> {
    Page<UserActivityLog> findByUser(User user, Pageable pageable);
    Page<UserActivityLog> findByUserAndCreatedAtBetween(User user, LocalDateTime start, LocalDateTime end, Pageable pageable);
    Page<UserActivityLog> findByActivityType(String activityType, Pageable pageable);
} 