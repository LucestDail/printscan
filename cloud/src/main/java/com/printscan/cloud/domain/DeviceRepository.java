package com.printscan.cloud.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {
    Optional<Device> findByDeviceToken(String token);
    List<Device> findByOrgIdOrderByIdAsc(Long orgId);
}
