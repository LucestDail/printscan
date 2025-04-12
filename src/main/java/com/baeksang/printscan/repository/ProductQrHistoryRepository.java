package com.baeksang.printscan.repository;

import com.baeksang.printscan.entity.Product;
import com.baeksang.printscan.entity.ProductQrHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductQrHistoryRepository extends JpaRepository<ProductQrHistory, Long> {
    List<ProductQrHistory> findByProduct(Product product);
    List<ProductQrHistory> findByProductOrderByGeneratedAtDesc(Product product);
} 