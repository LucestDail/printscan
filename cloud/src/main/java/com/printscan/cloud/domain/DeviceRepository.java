package com.printscan.cloud.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {
    Optional<Device> findByDeviceToken(String token);
    List<Device> findByOrgIdOrderByIdAsc(Long orgId);

    /** 인쇄 성공 집계 원자 증가(동시 ack lost-update 방지). */
    @Modifying
    @Query("update Device d set d.printCount = d.printCount + 1 where d.id = :id")
    int incrementPrintCount(@Param("id") Long id);
}
