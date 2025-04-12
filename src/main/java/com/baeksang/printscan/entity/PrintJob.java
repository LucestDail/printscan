package com.baeksang.printscan.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "print_jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrintJob extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private Integer copies;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PrintJobPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PrintJobStatus status;

    @Column(nullable = false)
    private Boolean doubleSided;

    @Column(nullable = false)
    private Boolean color;

    @Column(nullable = false)
    private Boolean stapled;

    @Column(length = 1000)
    private String notes;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    @OneToMany(mappedBy = "printJob", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PrintJobFile> files = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = PrintJobStatus.PENDING;
        }
    }

    public void start() {
        if (status == PrintJobStatus.PENDING) {
            status = PrintJobStatus.PROCESSING;
            startedAt = LocalDateTime.now();
        }
    }

    public void complete() {
        if (status == PrintJobStatus.PROCESSING) {
            status = PrintJobStatus.COMPLETED;
            completedAt = LocalDateTime.now();
        }
    }

    public void fail() {
        status = PrintJobStatus.FAILED;
        completedAt = LocalDateTime.now();
    }

    public void cancel() {
        if (status == PrintJobStatus.PENDING || status == PrintJobStatus.PROCESSING) {
            status = PrintJobStatus.CANCELED;
            completedAt = LocalDateTime.now();
        }
    }

    public void addFile(PrintJobFile file) {
        files.add(file);
        file.setPrintJob(this);
    }

    public void removeFile(PrintJobFile file) {
        files.remove(file);
        file.setPrintJob(null);
    }
} 