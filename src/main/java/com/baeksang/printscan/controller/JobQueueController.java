package com.baeksang.printscan.controller;

import com.baeksang.printscan.entity.JobQueue;
import com.baeksang.printscan.entity.User;
import com.baeksang.printscan.service.JobQueueService;
import com.baeksang.printscan.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobQueueController {

    private final JobQueueService jobQueueService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<Page<JobQueue>> getUserJobs(
            @AuthenticationPrincipal UserDetails userDetails,
            Pageable pageable) {
        User user = userService.findUserByUsername(userDetails.getUsername());
        return ResponseEntity.ok(jobQueueService.getUserJobs(user, pageable));
    }

    @GetMapping("/type/{jobType}")
    public ResponseEntity<Page<JobQueue>> getUserJobsByType(
            @PathVariable String jobType,
            @AuthenticationPrincipal UserDetails userDetails,
            Pageable pageable) {
        User user = userService.findUserByUsername(userDetails.getUsername());
        return ResponseEntity.ok(jobQueueService.getUserJobsByType(user, jobType, pageable));
    }
} 