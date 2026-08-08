package com.printscan.cloud.service;

import com.printscan.cloud.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
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
    private final ConsumptionLogRepository consumptions;

    // ── 디바이스 등록(아웃바운드) ──
    @Transactional
    public Device register(String orgApiKey, String name, String printerMode, String line) {
        Organization org = orgs.findByApiKey(orgApiKey)
                .orElseThrow(() -> new IllegalArgumentException("잘못된 orgApiKey"));
        Device d = new Device();
        d.setOrgId(org.getId());
        d.setName(name != null ? name : "device");
        d.setLine(line);
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

    // ── 폴링: 다음 QUEUED 잡을 원자적으로 클레임(→SENT) ──
    @Transactional
    public PrintJobCloud pollNext(Device d) {
        d.setLastSeenAt(LocalDateTime.now());
        devices.save(d);
        PrintJobCloud candidate = jobs.findFirstByDeviceIdAndStatusOrderByIdAsc(d.getId(), PrintJobCloud.Status.QUEUED)
                .orElse(null);
        if (candidate == null) return null;
        int claimed = jobs.claim(candidate.getId(), LocalDateTime.now());
        if (claimed == 0) return null; // 동시 폴이 먼저 가져감 → 이번엔 없음
        log.info("[fleet] device {} 폴링 → job {} 전달", d.getId(), candidate.getId());
        return jobs.findById(candidate.getId()).orElse(null); // SENT 로 재로딩
    }

    // ── 인쇄 결과 ack (SENT → DONE/FAILED, 멱등) ──
    @Transactional
    public void ack(Device d, Long jobId, boolean ok, String message) {
        PrintJobCloud job = jobs.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("job 없음: " + jobId));
        if (!job.getDeviceId().equals(d.getId())) throw new IllegalArgumentException("소유 불일치");
        if (job.getStatus() != PrintJobCloud.Status.SENT) {
            log.info("[fleet] job {} ack 무시(상태={})", jobId, job.getStatus()); // 재ack/중복 방지
            return;
        }
        job.setStatus(ok ? PrintJobCloud.Status.DONE : PrintJobCloud.Status.FAILED);
        job.setMessage(message);
        job.setDoneAt(LocalDateTime.now());
        jobs.save(job);
        if (ok) devices.incrementPrintCount(d.getId()); // 원자 증가
        log.info("[fleet] job {} ack={} ({})", jobId, ok, message);
    }

    // ── 리퍼: SENT 로 60초 이상 정체된 잡 재큐(디바이스가 ack 전 죽은 경우) ──
    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void requeueStaleJobs() {
        int n = jobs.requeueStale(LocalDateTime.now().minusSeconds(60));
        if (n > 0) log.info("[fleet] SENT 정체 잡 {}건 재큐", n);
    }

    // ── 하트비트 + 재고 업싱크 ──
    @Transactional
    public void heartbeat(Device d, String printerMode, String line, List<Map<String, Object>> inventory) {
        d.setLastSeenAt(LocalDateTime.now());
        if (printerMode != null) d.setPrinterMode(printerMode);
        if (line != null) d.setLine(line);
        devices.save(d);
        if (inventory != null) {
            for (Map<String, Object> row : inventory) {
                String code = String.valueOf(row.get("code"));
                InventorySnapshot s = snapshots.findByDeviceIdAndCode(d.getId(), code)
                        .orElseGet(InventorySnapshot::new);
                s.setOrgId(d.getOrgId());
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
    public PrintJobCloud enqueuePrint(Long callerOrgId, Long deviceId, double widthMm, double heightMm, Integer dpi,
                                      String elementsJson, String variablesJson, int copies,
                                      String seqVar, String serialPrefix, Integer serialStart,
                                      Integer serialCount, Integer serialPad) {
        Device d = devices.findById(deviceId).orElseThrow(() -> new IllegalArgumentException("device 없음"));
        if (!d.getOrgId().equals(callerOrgId)) throw new IllegalArgumentException("이 조직의 장비가 아닙니다."); // 테넌트 격리
        PrintJobCloud job = new PrintJobCloud();
        job.setOrgId(d.getOrgId());
        job.setDeviceId(deviceId);
        job.setWidthMm(widthMm);
        job.setHeightMm(heightMm);
        job.setDpi(dpi);
        job.setElementsJson(elementsJson);
        job.setVariablesJson(variablesJson);
        job.setCopies(copies > 0 ? copies : 1);
        job.setSeqVar(seqVar);
        job.setSerialPrefix(serialPrefix);
        job.setSerialStart(serialStart);
        job.setSerialCount(serialCount);
        job.setSerialPad(serialPad);
        return jobs.save(job);
    }

    // ── 조회/집계 (전부 org 스코프 — 테넌트 격리) ──
    @Transactional(readOnly = true)
    public List<Device> allDevices(Long orgId) { return devices.findByOrgIdOrderByIdAsc(orgId); }

    @Transactional(readOnly = true)
    public List<PrintJobCloud> recentJobs(Long orgId) { return jobs.findTop20ByOrgIdOrderByIdDesc(orgId); }

    @Transactional(readOnly = true)
    public List<InventorySnapshot> allSnapshots(Long orgId) { return snapshots.findByOrgIdOrderByUpdatedAtDesc(orgId); }

    // ── 소비(출고) 업싱크 + 집계 ──
    @Transactional
    public void recordConsumption(Device d, String code, int qty, String operator, String line, boolean fromPrint) {
        ConsumptionLog c = new ConsumptionLog();
        c.setOrgId(d.getOrgId());
        c.setDeviceId(d.getId());
        c.setLine(line != null && !line.isBlank() ? line : d.getLine());
        c.setOperator(operator);
        c.setCode(code);
        c.setQty(qty);
        c.setFromPrint(fromPrint);
        consumptions.save(c);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> consumption(Long orgId) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("byLine", toMap(consumptions.sumByLine(orgId)));
        out.put("byOperator", toMap(consumptions.sumByOperator(orgId)));
        out.put("byProduct", toMap(consumptions.sumByProduct(orgId)));
        out.put("total", consumptions.totalQty(orgId));
        return out;
    }

    /** SQL 집계 결과(List<[key, sum]>)를 순서 유지 맵으로. */
    private Map<String, Long> toMap(List<Object[]> rows) {
        Map<String, Long> m = new LinkedHashMap<>();
        for (Object[] r : rows) {
            String key = r[0] == null ? "(미지정)" : String.valueOf(r[0]);
            long val = r[1] == null ? 0L : ((Number) r[1]).longValue();
            m.put(key, val);
        }
        return m;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> stats(Long orgId) {
        List<Device> ds = devices.findByOrgIdOrderByIdAsc(orgId);
        long totalPrinted = ds.stream().mapToLong(Device::getPrintCount).sum();
        long online = ds.stream().filter(Device::isOnline).count();
        return Map.of(
                "devices", ds.size(),
                "online", online,
                "totalPrinted", totalPrinted,
                "queued", jobs.countByOrgIdAndStatus(orgId, PrintJobCloud.Status.QUEUED)
        );
    }
}
