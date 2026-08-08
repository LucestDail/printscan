package com.printscan.cloud.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface InventorySnapshotRepository extends JpaRepository<InventorySnapshot, Long> {
    Optional<InventorySnapshot> findByDeviceIdAndCode(Long deviceId, String code);
    List<InventorySnapshot> findByOrgIdOrderByUpdatedAtDesc(Long orgId);
}
