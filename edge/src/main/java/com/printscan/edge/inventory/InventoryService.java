package com.printscan.edge.inventory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 제품 CRUD + 스캔 조회 + 입출고 트랜잭션(이력 append + 이벤트 발행 → 클라우드 업싱크 훅). */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final ProductRepository products;
    private final InventoryMovementRepository movements;
    private final ApplicationEventPublisher events;

    // ── 제품 ──────────────────────────────
    @Transactional(readOnly = true)
    public List<Product> findAll() { return products.findAllByOrderByUpdatedAtDesc(); }

    @Transactional(readOnly = true)
    public Product byCode(String code) {
        return products.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("제품 없음(code=" + code + ")"));
    }

    @Transactional
    public Product save(Product p) {
        if (p.getId() == null) {
            products.findByCode(p.getCode()).ifPresent(x -> { throw new IllegalArgumentException("이미 존재하는 code: " + p.getCode()); });
        }
        return products.save(p);
    }

    @Transactional
    public void delete(Long id) { products.deleteById(id); }

    // ── 스캔 조회 ──────────────────────────────
    @Transactional(readOnly = true)
    public Product lookup(String code) {
        return products.findByCode(code).orElse(null); // 미등록이면 null(프론트에서 등록 유도)
    }

    // ── 입출고 ──────────────────────────────
    @Transactional
    public InventoryMovement move(String code, InventoryMovement.Type type, int qty, String note) {
        return move(code, type, qty, note, null, null, false);
    }

    @Transactional
    public InventoryMovement move(String code, InventoryMovement.Type type, int qty, String note,
                                  String operator, String line, boolean fromPrint) {
        Product p = byCode(code);
        int before = p.getQuantity();
        int abs = Math.abs(qty);
        int applied;
        switch (type) {
            case IN -> { products.applyDelta(p.getId(), abs); applied = abs; }
            case OUT -> {
                if (products.applyDelta(p.getId(), -abs) == 0)  // 원자적: 음수면 미적용
                    throw new IllegalArgumentException("재고가 부족합니다(현재=" + before + ", 출고=" + abs + ")");
                applied = -abs;
            }
            case ADJUST -> { products.setQuantity(p.getId(), qty); applied = qty - before; }
            default -> throw new IllegalArgumentException("알 수 없는 유형: " + type);
        }
        Product fresh = products.findById(p.getId()).orElseThrow();
        InventoryMovement m = record(fresh, type, applied, fresh.getQuantity(), note, operator, line, fromPrint);
        log.info("[inventory] {} {} {} → {} (op={}, line={})", type, code, applied, fresh.getQuantity(), operator, line);
        return m;
    }

    /**
     * 인쇄=자동 출고. 등록 제품이면 인쇄 매수만큼 원자적 OUT(0 클램프, 인쇄는 막지 않음). 미등록이면 no-op.
     */
    @Transactional
    public InventoryMovement consumeForPrint(String code, int qty, String operator, String line) {
        Product p = products.findByCode(code).orElse(null);
        if (p == null || qty <= 0) return null;
        int before = p.getQuantity();
        products.clampSubtract(p.getId(), qty);            // 원자적 차감(0 클램프)
        Product fresh = products.findById(p.getId()).orElseThrow();
        int applied = fresh.getQuantity() - before;        // ≤ 0
        InventoryMovement m = record(fresh, InventoryMovement.Type.OUT, applied, fresh.getQuantity(),
                "print", operator, line, true);
        log.info("[inventory] 인쇄 자동출고 {} {} → {} (op={}, line={})", code, applied, fresh.getQuantity(), operator, line);
        return m;
    }

    private InventoryMovement record(Product p, InventoryMovement.Type type, int applied, int result,
                                     String note, String operator, String line, boolean fromPrint) {
        InventoryMovement m = new InventoryMovement();
        m.setProductId(p.getId());
        m.setCode(p.getCode());
        m.setType(type);
        m.setDelta(applied);
        m.setResultQty(result);
        m.setNote(note);
        m.setOperator(operator);
        m.setLine(line);
        m.setFromPrint(fromPrint);
        movements.save(m);
        events.publishEvent(new InventoryMovedEvent(m));
        return m;
    }

    @Transactional(readOnly = true)
    public List<InventoryMovement> history(int limit) {
        return movements.findAllByOrderByAtDesc(PageRequest.of(0, limit));
    }

    /** 재고 변동 이벤트 — P4 클라우드 동기화 클라이언트가 구독해 업싱크. */
    public record InventoryMovedEvent(InventoryMovement movement) {}
}
