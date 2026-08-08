package com.printscan.cloud.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PrintJobRepository extends JpaRepository<PrintJobCloud, Long> {
    Optional<PrintJobCloud> findFirstByDeviceIdAndStatusOrderByIdAsc(Long deviceId, PrintJobCloud.Status status);
    List<PrintJobCloud> findTop20ByOrgIdOrderByIdDesc(Long orgId);
    long countByOrgIdAndStatus(Long orgId, PrintJobCloud.Status status);

    /** 원자적 잡 클레임: QUEUED 인 경우에만 SENT 로 전이. 반환 1=내가 획득, 0=다른 폴이 가져감. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update PrintJobCloud j set j.status = com.printscan.cloud.domain.PrintJobCloud.Status.SENT, j.sentAt = :now "
            + "where j.id = :id and j.status = com.printscan.cloud.domain.PrintJobCloud.Status.QUEUED")
    int claim(@Param("id") Long id, @Param("now") LocalDateTime now);

    /** SENT 상태로 멈춘 잡(디바이스가 ack 전에 죽음) 재큐 — 리퍼. */
    @Modifying
    @Query("update PrintJobCloud j set j.status = com.printscan.cloud.domain.PrintJobCloud.Status.QUEUED "
            + "where j.status = com.printscan.cloud.domain.PrintJobCloud.Status.SENT and j.sentAt < :cutoff")
    int requeueStale(@Param("cutoff") LocalDateTime cutoff);
}
