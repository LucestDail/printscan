package com.baeksang.printscan.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Product extends BaseTimeEntity {

    @Id
    private String id;  // UUID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ProductCategory category;

    @Column(nullable = false)
    private String producerId;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String unit;

    @Column(nullable = false)
    private String qrCode;

    @Column(nullable = false)
    private Integer minStockQuantity;

    @Column(nullable = false)
    private Integer maxStockQuantity;

    @Column(nullable = false)
    private LocalDateTime producedAt;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ProductStatus status;

    public enum ProductStatus {
        CREATED, IN_STOCK, SHIPPED, DELIVERED, DISPOSED
    }

    // Setter methods
    public void setStatus(ProductStatus status) {
        this.status = status;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }
} 