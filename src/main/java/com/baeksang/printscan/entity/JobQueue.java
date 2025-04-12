package com.baeksang.printscan.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "job_queue")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private FileInfo fileInfo;

    @Column(nullable = false)
    private String jobType;  // PRINT, SCAN

    @Column(nullable = false)
    private String jobStatus;  // PENDING, PROCESSING, COMPLETED, FAILED

    private String jobOptions;  // JSON 형식의 작업 옵션 (용지 크기, 방향 등)

    private Integer priority;  // 작업 우선순위

    private String errorMessage;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        priority = priority != null ? priority : 0;
        jobStatus = "PENDING";
    }

    public void updateStatus(String status) {
        this.jobStatus = status;
        if ("PROCESSING".equals(status)) {
            this.startedAt = LocalDateTime.now();
        } else if ("COMPLETED".equals(status) || "FAILED".equals(status)) {
            this.completedAt = LocalDateTime.now();
        }
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
} 