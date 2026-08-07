package com.printscan.edge.inventory;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 재고 변동 이력(append). 스캔 입출고/조정 기록 — 클라우드 업싱크 대상. */
@Entity
@Table(name = "inventory_movement", indexes = @Index(name = "ix_move_at", columnList = "at"))
@Getter
@Setter
public class InventoryMovement {

    public enum Type { IN, OUT, ADJUST }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long productId;
    private String code;

    @Enumerated(EnumType.STRING)
    private Type type;

    private int delta;        // 변동량(부호 포함)
    private int resultQty;    // 변동 후 재고
    private String note;

    private String operator;   // 작업자(소비 귀속)
    private String line;       // 생산 라인
    private Boolean fromPrint; // 인쇄로 인한 자동 출고 여부(nullable — 기존행 호환)

    private LocalDateTime at;

    @PrePersist void onCreate() { if (at == null) at = LocalDateTime.now(); }
}
