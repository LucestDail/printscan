package com.baeksang.printscan.controller;

import com.baeksang.printscan.entity.Product;
import com.baeksang.printscan.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestParam String producerId, @RequestParam String qrCode) {
        return ResponseEntity.ok(productService.createProductWithQR(producerId, qrCode));
    }

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productService.findProductsByStatus(Product.ProductStatus.CREATED));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable String id) {
        return ResponseEntity.ok(productService.findProductById(id));
    }

    @GetMapping("/qr/{qrCode}")
    public ResponseEntity<Product> getProductByQrCode(@PathVariable String qrCode) {
        return ResponseEntity.ok(productService.findProductByQrCode(qrCode));
    }
}