package com.printscan.edge.inventory;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {
    List<InventoryMovement> findByProductIdOrderByAtDesc(Long productId);
    List<InventoryMovement> findAllByOrderByAtDesc(Pageable pageable);
}
