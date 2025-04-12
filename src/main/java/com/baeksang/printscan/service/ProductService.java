package com.baeksang.printscan.service;

import com.baeksang.printscan.entity.Product;
import com.baeksang.printscan.entity.ProductCategory;
import com.baeksang.printscan.entity.ProductQrHistory;
import com.baeksang.printscan.entity.User;
import com.baeksang.printscan.exception.BusinessException;
import com.baeksang.printscan.exception.EntityNotFoundException;
import com.baeksang.printscan.repository.ProductCategoryRepository;
import com.baeksang.printscan.repository.ProductQrHistoryRepository;
import com.baeksang.printscan.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;
    private final ProductQrHistoryRepository qrHistoryRepository;

    @Transactional
    public Product createProductWithQR(String producerId, String qrCode) {
        Product product = Product.builder()
                .id(UUID.randomUUID().toString())
                .producerId(producerId)
                .qrCode(qrCode)
                .producedAt(LocalDateTime.now())
                .status(Product.ProductStatus.CREATED)
                .build();

        return productRepository.save(product);
    }

    @Transactional
    public Product createProduct(String producerId, String code, String name, Long categoryId,
                               String description, String unit, Integer minStock, Integer maxStock) {
        // 카테고리 존재 확인
        ProductCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("ProductCategory", categoryId));

        // 제품 코드 중복 확인
        if (productRepository.existsByCode(code)) {
            throw new BusinessException("Product code already exists: " + code);
        }

        // QR 코드 생성
        String qrCode = generateQrCode();

        // 제품 생성
        Product product = Product.builder()
                .id(UUID.randomUUID().toString())
                .producerId(producerId)
                .code(code)
                .name(name)
                .category(category)
                .description(description)
                .unit(unit)
                .qrCode(qrCode)
                .minStockQuantity(minStock)
                .maxStockQuantity(maxStock)
                .producedAt(LocalDateTime.now())
                .status(Product.ProductStatus.CREATED)
                .build();

        return productRepository.save(product);
    }

    @Transactional
    public Product updateProductStatus(String productId, Product.ProductStatus newStatus) {
        Product product = findProductById(productId);
        product.setStatus(newStatus);
        return productRepository.save(product);
    }

    @Transactional
    public ProductQrHistory regenerateQrCode(String productId, User user) {
        Product product = findProductById(productId);
        String newQrCode = generateQrCode();

        // QR 코드 이력 저장
        ProductQrHistory history = ProductQrHistory.builder()
                .product(product)
                .qrCode(product.getQrCode()) // 이전 QR 코드 저장
                .generatedAt(LocalDateTime.now())
                .createdBy(user)
                .note("QR 코드 재발급")
                .build();

        // 제품의 QR 코드 업데이트
        product.setQrCode(newQrCode);
        productRepository.save(product);

        return qrHistoryRepository.save(history);
    }

    public Product findProductById(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product", id));
    }

    public Product findProductByQrCode(String qrCode) {
        return productRepository.findByQrCode(qrCode)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with QR code: " + qrCode));
    }

    public List<Product> findProductsByCategory(Long categoryId) {
        return productRepository.findByCategoryId(categoryId);
    }

    public List<Product> findProductsByStatus(Product.ProductStatus status) {
        return productRepository.findByStatus(status);
    }

    private String generateQrCode() {
        // QR 코드 생성 로직 구현 필요
        return UUID.randomUUID().toString();
    }
}