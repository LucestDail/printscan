package com.printscan.cloud.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 테넌트. apiKey 로 디바이스가 자기 조직에 등록한다. 로테이션 시 직전 키는 유예기간 동안만 유효. */
@Entity
@Table(name = "organization")
@Getter
@Setter
public class Organization {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String apiKey;

    /** 로테이션 직전 키 — previousKeyExpiresAt 이전까지만 유효(무중단 교체용). null=유예 없음. */
    private String previousApiKey;

    /** 직전 키 만료 시각. now 이후면 직전 키 거부. */
    private LocalDateTime previousKeyExpiresAt;

    /** 마지막 로테이션 시각(감사용). */
    private LocalDateTime keyRotatedAt;
}
