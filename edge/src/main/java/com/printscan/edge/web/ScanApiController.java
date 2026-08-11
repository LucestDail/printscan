package com.printscan.edge.web;

import com.printscan.edge.config.LineProperties;
import com.printscan.edge.inventory.InventoryMovement;
import com.printscan.edge.inventory.InventoryService;
import com.printscan.edge.inventory.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 스캔/재고 REST — 제품 CRUD + 스캔 조회 + 입출고. */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ScanApiController {

    private final InventoryService service;
    private final LineProperties lineProps;

    // ── 제품 ──
    @GetMapping("/products")
    public List<Product> products() { return service.findAll(); }

    @PostMapping("/products")
    public Product createProduct(@RequestBody Product p) {
        // 예외는 GlobalExceptionHandler 가 요청 로케일로 번역(자체 catch 금지 — 원시 코드 노출 방지).
        return service.save(p);
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ── 스캔 조회 ──
    @GetMapping("/scan/lookup")
    public ResponseEntity<?> lookup(@RequestParam String code) {
        Product p = service.lookup(code.trim());
        if (p == null) return ResponseEntity.status(404).body(Map.of("found", false, "code", code));
        return ResponseEntity.ok(p);
    }

    // ── 입출고 ──
    @PostMapping("/inventory/move")
    public InventoryMovement move(@RequestBody MoveRequest req) {
        // 잘못된 type → enum valueOf 가 IllegalArgumentException → 핸들러가 error.badRequest 로 번역.
        InventoryMovement.Type t = InventoryMovement.Type.valueOf(req.type().toUpperCase());
        return service.move(req.code().trim(), t, req.qty(),
                req.note(), req.operator(), lineProps.getName(), false);
    }

    @GetMapping("/inventory/history")
    public List<InventoryMovement> history(@RequestParam(defaultValue = "50") int limit) {
        return service.history(limit);
    }

    public record MoveRequest(String code, String type, int qty, String note, String operator) {}
}
