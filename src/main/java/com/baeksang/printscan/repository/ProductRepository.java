package com.baeksang.printscan.repository;

import com.baeksang.printscan.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, String> {
    Optional<Product> findByQrCode(String qrCode);
    Optional<Product> findByCode(String code);
    List<Product> findByStatus(Product.ProductStatus status);
    
    @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId")
    List<Product> findByCategoryId(@Param("categoryId") Long categoryId);
    
    boolean existsByCode(String code);
    boolean existsByQrCode(String qrCode);
}
