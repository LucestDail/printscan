package com.printscan.cloud.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 디바이스가 업싱크한 소비(출고) 로그. 라인/작업자/제품별 집계의 원천. */
@Entity
@Table(name = "consumption_log", indexes = @Index(name = "ix_consume_at", columnList = "at"))
@Getter
@Setter
public class ConsumptionLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orgId;
    private Long deviceId;
    private String line;
    private String operator;
    private String code;
    private int qty;
    private boolean fromPrint;
    private LocalDateTime at;

    @PrePersist void onCreate() { if (at == null) at = LocalDateTime.now(); }
}
