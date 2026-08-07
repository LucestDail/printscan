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
        Product p = byCode(code);
        int delta = switch (type) {
            case IN -> Math.abs(qty);
            case OUT -> -Math.abs(qty);
            case ADJUST -> qty; // 절대 조정: qty 를 목표치로 → delta 계산
        };
        int result = (type == InventoryMovement.Type.ADJUST) ? qty : p.getQuantity() + delta;
        if (result < 0) throw new IllegalArgumentException("재고가 음수가 됩니다(현재=" + p.getQuantity() + ", 요청=" + delta + ")");

        int applied = (type == InventoryMovement.Type.ADJUST) ? (qty - p.getQuantity()) : delta;
        p.setQuantity(result);
        products.save(p);

        InventoryMovement m = new InventoryMovement();
        m.setProductId(p.getId());
        m.setCode(p.getCode());
        m.setType(type);
        m.setDelta(applied);
        m.setResultQty(result);
        m.setNote(note);
        movements.save(m);

        events.publishEvent(new InventoryMovedEvent(m));
        log.info("[inventory] {} {} {} → {}", type, p.getCode(), applied, result);
        return m;
    }

    @Transactional(readOnly = true)
    public List<InventoryMovement> history(int limit) {
        return movements.findAllByOrderByAtDesc(PageRequest.of(0, limit));
    }

    /** 재고 변동 이벤트 — P4 클라우드 동기화 클라이언트가 구독해 업싱크. */
    public record InventoryMovedEvent(InventoryMovement movement) {}
}
