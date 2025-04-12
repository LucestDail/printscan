package com.baeksang.printscan.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "print_job_files")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrintJobFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "print_job_id")
    private PrintJob printJob;

    @Column(nullable = false)
    private String originalFilename;

    @Column(nullable = false)
    private String storedFilename;

    @Column(nullable = false)
    private Long fileSize;
} 