package com.printscan.cloud.service;

import com.printscan.cloud.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 플릿 관리 핵심 — 디바이스 등록/폴링/ack/하트비트/재고 업싱크 + 네트워크 출력 큐잉/집계. */
@Slf4j
@Service
@RequiredArgsConstructor
public class FleetService {

    private final OrganizationRepository orgs;
    private final DeviceRepository devices;
    private final PrintJobRepository jobs;
    private final InventorySnapshotRepository snapshots;

    // ── 디바이스 등록(아웃바운드) ──
    @Transactional
    public Device register(String orgApiKey, String name, String printerMode) {
        Organization org = orgs.findByApiKey(orgApiKey)
                .orElseThrow(() -> new IllegalArgumentException("잘못된 orgApiKey"));
        Device d = new Device();
        d.setOrgId(org.getId());
        d.setName(name != null ? name : "device");
        d.setDeviceToken("DEV-" + UUID.randomUUID().toString().replace("-", ""));
        d.setPrinterMode(printerMode);
        d.setLastSeenAt(LocalDateTime.now());
        return devices.save(d);
    }

    @Transactional(readOnly = true)
    public Device authDevice(String token) {
        return devices.findByDeviceToken(token)
                .orElseThrow(() -> new IllegalArgumentException("잘못된 deviceToken"));
    }

    // ── 폴링: 다음 QUEUED 잡 수령(→SENT) ──
    @Transactional
    public PrintJobCloud pollNext(Device d) {
        d.setLastSeenAt(LocalDateTime.now());
        devices.save(d);
        PrintJobCloud job = jobs.findFirstByDeviceIdAndStatusOrderByIdAsc(d.getId(), PrintJobCloud.Status.QUEUED)
                .orElse(null);
        if (job != null) {
            job.setStatus(PrintJobCloud.Status.SENT);
            job.setSentAt(LocalDateTime.now());
            jobs.save(job);
            log.info("[fleet] device {} 폴링 → job {} 전달", d.getId(), job.getId());
        }
        return job;
    }

    // ── 인쇄 결과 ack ──
    @Transactional
    public void ack(Device d, Long jobId, boolean ok, String message) {
        PrintJobCloud job = jobs.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("job 없음: " + jobId));
        if (!job.getDeviceId().equals(d.getId())) throw new IllegalArgumentException("소유 불일치");
        job.setStatus(ok ? PrintJobCloud.Status.DONE : PrintJobCloud.Status.FAILED);
        job.setMessage(message);
        job.setDoneAt(LocalDateTime.now());
        jobs.save(job);
        if (ok) { d.setPrintCount(d.getPrintCount() + 1); devices.save(d); }
        log.info("[fleet] job {} ack={} ({})", jobId, ok, message);
    }

    // ── 하트비트 + 재고 업싱크 ──
    @Transactional
    public void heartbeat(Device d, String printerMode, List<Map<String, Object>> inventory) {
        d.setLastSeenAt(LocalDateTime.now());
        if (printerMode != null) d.setPrinterMode(printerMode);
        devices.save(d);
        if (inventory != null) {
            for (Map<String, Object> row : inventory) {
                String code = String.valueOf(row.get("code"));
                InventorySnapshot s = snapshots.findByDeviceIdAndCode(d.getId(), code)
                        .orElseGet(InventorySnapshot::new);
                s.setDeviceId(d.getId());
                s.setCode(code);
                s.setName(String.valueOf(row.getOrDefault("name", "")));
                s.setQuantity(((Number) row.getOrDefault("quantity", 0)).intValue());
                s.setUpdatedAt(LocalDateTime.now());
                snapshots.save(s);
            }
        }
    }

    // ── 네트워크 출력: 잡 큐잉 ──
    @Transactional
    public PrintJobCloud enqueuePrint(Long deviceId, double widthMm, double heightMm, Integer dpi,
                                      String elementsJson, String variablesJson, int copies) {
        Device d = devices.findById(deviceId).orElseThrow(() -> new IllegalArgumentException("device 없음"));
        PrintJobCloud job = new PrintJobCloud();
        job.setOrgId(d.getOrgId());
        job.setDeviceId(deviceId);
        job.setWidthMm(widthMm);
        job.setHeightMm(heightMm);
        job.setDpi(dpi);
        job.setElementsJson(elementsJson);
        job.setVariablesJson(variablesJson);
        job.setCopies(copies > 0 ? copies : 1);
        return jobs.save(job);
    }

    // ── 조회/집계 ──
    @Transactional(readOnly = true)
    public List<Device> allDevices() { return devices.findAll(); }

    @Transactional(readOnly = true)
    public List<PrintJobCloud> recentJobs() { return jobs.findTop20ByOrderByIdDesc(); }

    @Transactional(readOnly = true)
    public List<InventorySnapshot> allSnapshots() { return snapshots.findByOrderByUpdatedAtDesc(); }

    @Transactional(readOnly = true)
    public Map<String, Object> stats() {
        long totalPrinted = devices.findAll().stream().mapToLong(Device::getPrintCount).sum();
        long online = devices.findAll().stream().filter(Device::isOnline).count();
        return Map.of(
                "devices", devices.count(),
                "online", online,
                "totalPrinted", totalPrinted,
                "queued", jobs.countByStatus(PrintJobCloud.Status.QUEUED)
        );
    }
}
