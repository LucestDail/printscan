package com.baeksang.printscan.service;

import com.baeksang.printscan.model.Product;
import com.baeksang.printscan.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    public Product createProduct(String producerId, String qrCode) {
        Product product = new Product();
        product.setId(UUID.randomUUID().toString());
        product.setProducerId(producerId);
        product.setQrCode(qrCode);
        product.setProducedAt(LocalDateTime.now());
        product.setStatus("생산완료");
        productRepository.save(product);
        return product;
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
}