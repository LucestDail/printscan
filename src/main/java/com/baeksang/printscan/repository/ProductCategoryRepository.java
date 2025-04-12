package com.baeksang.printscan.repository;

import com.baeksang.printscan.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {
    Optional<ProductCategory> findByName(String name);
    boolean existsByName(String name);
} 