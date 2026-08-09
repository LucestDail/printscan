package com.printscan.cloud.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/** 조직(테넌트) 중앙 라벨 템플릿 — 허브에서 관리, 디바이스가 폴링으로 로컬 동기화. */
@Entity
@Table(name = "cloud_template", indexes = @Index(name = "ix_tpl_org", columnList = "orgId"))
@Getter
@Setter
public class CloudTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orgId;

    @Column(nullable = false)
    private String name;

    private double widthMm = 40;
    private double heightMm = 25;
    private Integer dpi;

    // 방언이 타입 결정(PG=text, H2=clob) — columnDefinition="CLOB" 하드코딩은 Postgres 에 없어 금지.
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String elementsJson = "[]";

    private LocalDateTime updatedAt;

    @PrePersist @PreUpdate void touch() { updatedAt = LocalDateTime.now(); }
}
