package com.printscan.edge.inventory;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 재고 동시성 — 원자적 UPDATE(applyDelta)가 lost-update 를 막는지 실부하로 증명.
 * 감사 1순위 지적(E3: read-modify-write 경쟁)의 수정이 동시 출고에서 유지됨을 검증.
 */
@SpringBootTest  // MOCK 웹 컨텍스트(포트 없음) — SecurityConfig 의 HttpSecurity 빈 필요
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:conctest;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "printscan.cloud.enabled=false"
})
class InventoryConcurrencyTest {

    @Autowired InventoryService service;
    @Autowired ProductRepository products;
    @Autowired InventoryMovementRepository movements;

    private void seed(String code, int qty) {
        Product p = new Product();
        p.setCode(code); p.setName("n"); p.setQuantity(qty);
        service.save(p);
    }

    /** N 스레드가 동시에 1씩 출고 → 정확히 N 감소(lost-update 0). */
    @Test
    void 동시_출고_lost_update_없음() throws Exception {
        seed("CC1", 200);
        int threads = 100;
        runConcurrent(threads, () -> service.move("CC1", InventoryMovement.Type.OUT, 1, "c"));

        int finalQty = products.findByCode("CC1").orElseThrow().getQuantity();
        assertEquals(100, finalQty, "200 - 100 = 100 이어야(lost-update 없음)");
        assertEquals(100, movements.findAllByOrderByAtDesc(org.springframework.data.domain.PageRequest.of(0, 1000)).size(),
                "출고 100건 이력");
    }

    /** 재고보다 많은 동시 출고 → 성공분만 차감, 절대 음수 안 됨. */
    @Test
    void 초과_출고_음수_방지() throws Exception {
        seed("CC2", 50);
        int threads = 100;
        AtomicInteger ok = new AtomicInteger(), fail = new AtomicInteger();
        runConcurrentCounting(threads, () -> service.move("CC2", InventoryMovement.Type.OUT, 1, "c"), ok, fail);

        int finalQty = products.findByCode("CC2").orElseThrow().getQuantity();
        assertEquals(0, finalQty, "정확히 50개만 나가고 0에서 멈춤");
        assertTrue(finalQty >= 0, "절대 음수 금지");
        assertEquals(50, ok.get(), "정확히 50건 성공");
        assertEquals(50, fail.get(), "나머지 50건은 재고부족 거부");
    }

    private void runConcurrent(int n, Runnable op) throws Exception {
        runConcurrentCounting(n, op, new AtomicInteger(), new AtomicInteger());
    }

    private void runConcurrentCounting(int n, Runnable op, AtomicInteger ok, AtomicInteger fail) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(16);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(n);
        for (int i = 0; i < n; i++) {
            pool.submit(() -> {
                try {
                    start.await();          // 동시 출발
                    op.run();
                    ok.incrementAndGet();
                } catch (Exception e) {
                    fail.incrementAndGet(); // 재고부족(ApiException) 등
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertTrue(done.await(30, TimeUnit.SECONDS), "30초 내 완료");
        pool.shutdownNow();
    }
}
