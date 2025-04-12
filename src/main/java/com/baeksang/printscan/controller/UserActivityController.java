package com.baeksang.printscan.controller;

import com.baeksang.printscan.entity.User;
import com.baeksang.printscan.entity.UserActivityLog;
import com.baeksang.printscan.service.UserActivityService;
import com.baeksang.printscan.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
public class UserActivityController {

    private final UserActivityService activityService;
    private final UserService userService;

    @GetMapping("/my")
    public ResponseEntity<Page<UserActivityLog>> getMyActivities(
            @AuthenticationPrincipal UserDetails userDetails,
            Pageable pageable) {
        User user = userService.findUserByUsername(userDetails.getUsername());
        return ResponseEntity.ok(activityService.getUserActivities(user, pageable));
    }

    @GetMapping("/my/date-range")
    public ResponseEntity<Page<UserActivityLog>> getMyActivitiesByDateRange(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            Pageable pageable) {
        User user = userService.findUserByUsername(userDetails.getUsername());
        return ResponseEntity.ok(activityService.getUserActivitiesByDateRange(user, start, end, pageable));
    }

    @GetMapping("/users/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserActivityLog>> getUserActivities(
            @PathVariable String username,
            Pageable pageable) {
        User user = userService.findUserByUsername(username);
        return ResponseEntity.ok(activityService.getUserActivities(user, pageable));
    }

    @GetMapping("/type/{activityType}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserActivityLog>> getActivitiesByType(
            @PathVariable String activityType,
            Pageable pageable) {
        return ResponseEntity.ok(activityService.getActivitiesByType(activityType, pageable));
    }
} 