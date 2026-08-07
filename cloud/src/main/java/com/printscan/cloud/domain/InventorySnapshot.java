package com.printscan.cloud.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 디바이스가 업싱크한 재고 스냅샷(디바이스+코드 단위 최신값). */
@Entity
@Table(name = "inventory_snapshot", uniqueConstraints = @UniqueConstraint(columnNames = {"deviceId", "code"}))
@Getter
@Setter
public class InventorySnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long deviceId;
    private String code;
    private String name;
    private int quantity;
    private LocalDateTime updatedAt;
}
