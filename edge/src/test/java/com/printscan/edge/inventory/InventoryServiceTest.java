package com.printscan.edge.inventory;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.junit.jupiter.api.Assertions.*;

/** 재고 입출고·조정·인쇄 자동출고(클램프) 회귀. */
@DataJpaTest
class InventoryServiceTest {

    @Autowired ProductRepository products;
    @Autowired InventoryMovementRepository movements;

    private InventoryService svc() {
        return new InventoryService(products, movements, event -> { /* no-op publisher */ },
                new com.printscan.edge.config.AlertService(""));
    }

    private Product product(String code, int qty) {
        Product p = new Product();
        p.setCode(code); p.setName("n"); p.setQuantity(qty);
        return products.save(p);
    }

    @Test
    void OUT_IN_델타() {
        product("A", 10);
        InventoryService s = svc();
        assertEquals(6, s.move("A", InventoryMovement.Type.OUT, 4, null).getResultQty());
        assertEquals(9, s.move("A", InventoryMovement.Type.IN, 3, null).getResultQty());
    }

    @Test
    void ADJUST_는_목표치로() {
        product("B", 9);
        InventoryService s = svc();
        InventoryMovement m = s.move("B", InventoryMovement.Type.ADJUST, 5, null);
        assertEquals(5, m.getResultQty());
        assertEquals(5 - 9, m.getDelta()); // 적용된 변동 = 목표-현재
    }

    @Test
    void 음수재고는_거부() {
        product("C", 3);
        InventoryService s = svc();
        assertThrows(IllegalArgumentException.class,
                () -> s.move("C", InventoryMovement.Type.OUT, 100, null));
    }

    @Test
    void 인쇄자동출고_클램프_비차단() {
        product("D", 2);
        InventoryService s = svc();
        InventoryMovement m = s.consumeForPrint("D", 5, "op", "line1"); // 요청 5 > 재고 2
        assertEquals(0, m.getResultQty(), "재고는 0으로 클램프");
        assertEquals(-2, m.getDelta());
        assertTrue(Boolean.TRUE.equals(m.getFromPrint()));
        assertEquals("op", m.getOperator());
        assertEquals("line1", m.getLine());
    }

    @Test
    void 미등록코드_자동출고는_noop() {
        InventoryService s = svc();
        assertNull(s.consumeForPrint("NOPE", 1, "op", "line1"));
    }
}
