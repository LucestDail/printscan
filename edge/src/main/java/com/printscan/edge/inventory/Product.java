package com.printscan.edge.inventory;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 제품/재고 마스터. code = 바코드/QR 스캔 키(유니크). */
@Entity
@Table(name = "product", indexes = @Index(name = "ux_product_code", columnList = "code", unique = true))
@Getter
@Setter
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    private String unit = "EA";

    private int quantity = 0;
    private int minQty = 0;
    private int maxQty = 0;

    @Column(updatable = false)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate  void onUpdate() { updatedAt = LocalDateTime.now(); }

    @Transient
    public boolean isLowStock() { return minQty > 0 && quantity <= minQty; }
}
