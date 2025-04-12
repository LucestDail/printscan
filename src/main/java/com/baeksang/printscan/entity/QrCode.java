package com.baeksang.printscan.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "qr_codes")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QrCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true)
    private String code;  // 고유한 QR 코드 값

    @Column(nullable = false)
    private String type;  // PRINT, SCAN

    @Column(nullable = false)
    private boolean used;  // QR 코드 사용 여부

    @Column(nullable = false)
    private LocalDateTime expiresAt;  // QR 코드 만료 시간

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime usedAt;  // QR 코드 사용 시간

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        used = false;
    }

    public void markAsUsed() {
        this.used = true;
        this.usedAt = LocalDateTime.now();
    }

    public boolean isValid() {
        return !used && LocalDateTime.now().isBefore(expiresAt);
    }
} 