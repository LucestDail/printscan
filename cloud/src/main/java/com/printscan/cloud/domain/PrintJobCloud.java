package com.printscan.cloud.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/** 네트워크 출력 잡. 클라우드가 QUEUED 생성 → 디바이스 폴링 시 SENT → 인쇄 후 ack 로 DONE/FAILED. */
@Entity
@Table(name = "print_job", indexes = @Index(name = "ix_job_device_status", columnList = "deviceId,status"))
@Getter
@Setter
public class PrintJobCloud {

    public enum Status { QUEUED, SENT, DONE, FAILED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orgId;
    private Long deviceId;

    @Enumerated(EnumType.STRING)
    private Status status = Status.QUEUED;

    // 라벨 정의(edge LabelService 가 동일 스키마로 렌더)
    private double widthMm;
    private double heightMm;
    private Integer dpi;
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String elementsJson;
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String variablesJson;
    private int copies = 1;

    // 일련번호 배치(있으면 edge 가 batch 인쇄). serialCount>1 → 배치.
    private String seqVar;
    private String serialPrefix;
    private Integer serialStart;
    private Integer serialCount;
    private Integer serialPad;

    private String message;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
    private LocalDateTime doneAt;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}
