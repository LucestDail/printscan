package com.printscan.cloud.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 등록된 온디바이스(Pi). deviceToken 으로 폴링/ack 인증. */
@Entity
@Table(name = "device")
@Getter
@Setter
public class Device {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orgId;
    private String name;
    private String line;   // 담당 생산 라인

    @Column(unique = true)
    private String deviceToken;

    private String printerMode;
    private LocalDateTime lastSeenAt;
    private LocalDateTime registeredAt;

    private long printCount;   // 누적 인쇄 성공 건수(집계)

    @PrePersist void onCreate() { registeredAt = LocalDateTime.now(); }

    @Transient
    public boolean isOnline() {
        return lastSeenAt != null && lastSeenAt.isAfter(LocalDateTime.now().minusSeconds(60));
    }
}
