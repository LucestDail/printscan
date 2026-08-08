package com.printscan.edge.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByCode(String code);
    List<Product> findAllByOrderByUpdatedAtDesc();

    /** 원자적 재고 증감(음수 방지). 반환 1=적용, 0=재고 부족(또는 없음) → lost-update 없음. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Product p set p.quantity = p.quantity + :delta, p.updatedAt = CURRENT_TIMESTAMP "
            + "where p.id = :id and p.quantity + :delta >= 0")
    int applyDelta(@Param("id") Long id, @Param("delta") int delta);

    /** 원자적 차감(0으로 클램프). 인쇄 자동출고용 — 인쇄를 막지 않음. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Product p set p.quantity = case when p.quantity - :qty < 0 then 0 else p.quantity - :qty end, "
            + "p.updatedAt = CURRENT_TIMESTAMP where p.id = :id")
    int clampSubtract(@Param("id") Long id, @Param("qty") int qty);

    /** 원자적 절대 설정(재고 조정). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Product p set p.quantity = :target, p.updatedAt = CURRENT_TIMESTAMP where p.id = :id")
    int setQuantity(@Param("id") Long id, @Param("target") int target);
}
