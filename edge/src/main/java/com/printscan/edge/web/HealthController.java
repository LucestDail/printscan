package com.printscan.edge.web;

import com.printscan.edge.config.LineProperties;
import com.printscan.edge.config.PrinterProperties;
import com.printscan.edge.inventory.InventoryService;
import com.printscan.edge.inventory.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 온디바이스 상태 요약 — 홈 화면/모니터링용(관측성). */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HealthController {

    private final PrinterProperties printer;
    private final LineProperties line;
    private final InventoryService inventory;

    @GetMapping("/health")
    public Map<String, Object> health() {
        List<Product> products = inventory.findAll();
        long low = products.stream().filter(Product::isLowStock).count();
        return Map.of(
                "status", "UP",
                "line", line.getName(),
                "printerMode", printer.getMode(),
                "printerName", printer.getName(),
                "products", products.size(),
                "lowStock", low
        );
    }
}
