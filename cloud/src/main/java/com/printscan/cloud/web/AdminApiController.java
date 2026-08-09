package com.printscan.cloud.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.printscan.cloud.domain.*;
import com.printscan.cloud.service.FleetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 관리자 UI — 전부 X-Org-Key 로 테넌트 스코프(온프렘 단일org면 헤더 생략 가능).
 * 조회/집계/네트워크출력 모두 호출자 org 로 한정 → 테넌트 격리.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminApiController {

    private final FleetService fleet;
    private final OrgContext orgContext;
    private final ObjectMapper mapper = new ObjectMapper();

    private static final String ORG = "X-Org-Key";

    @GetMapping("/devices")
    public List<Device> devices(@RequestHeader(value = ORG, required = false) String k) {
        return fleet.allDevices(orgContext.resolve(k).getId());
    }

    @GetMapping("/jobs")
    public List<PrintJobCloud> jobs(@RequestHeader(value = ORG, required = false) String k) {
        return fleet.recentJobs(orgContext.resolve(k).getId());
    }

    @GetMapping("/snapshots")
    public List<InventorySnapshot> snapshots(@RequestHeader(value = ORG, required = false) String k) {
        return fleet.allSnapshots(orgContext.resolve(k).getId());
    }

    @GetMapping("/stats")
    public Map<String, Object> stats(@RequestHeader(value = ORG, required = false) String k) {
        return fleet.stats(orgContext.resolve(k).getId());
    }

    @GetMapping("/consumption")
    public Map<String, Object> consumption(@RequestHeader(value = ORG, required = false) String k) {
        return fleet.consumption(orgContext.resolve(k).getId());
    }

    // ── 중앙 템플릿(테넌트 스코프) ──
    @GetMapping("/templates")
    public List<CloudTemplate> templates(@RequestHeader(value = ORG, required = false) String k) {
        return fleet.listTemplates(orgContext.resolve(k).getId());
    }

    @PostMapping("/templates")
    public CloudTemplate saveTemplate(@RequestHeader(value = ORG, required = false) String k,
                                      @RequestBody CloudTemplate t) {
        return fleet.saveTemplate(orgContext.resolve(k).getId(), t);
    }

    @DeleteMapping("/templates/{id}")
    public ResponseEntity<Void> deleteTemplate(@RequestHeader(value = ORG, required = false) String k,
                                               @PathVariable Long id) {
        fleet.deleteTemplate(orgContext.resolve(k).getId(), id);
        return ResponseEntity.noContent().build();
    }

    /** 네트워크 출력 지시 — 대상 장비가 호출자 org 소속이어야 함(격리). */
    @PostMapping("/devices/{id}/print")
    public PrintJobCloud print(@RequestHeader(value = ORG, required = false) String k,
                               @PathVariable Long id, @RequestBody NetworkPrintRequest req) throws Exception {
        Long orgId = orgContext.resolve(k).getId();
        // 입력 상한: 네트워크 출력 증폭(물리 DoS) 방지
        if (req.copies() != null && req.copies() > 1000) throw new IllegalArgumentException("매수 상한 초과(≤1000)");
        if (req.serialCount() != null && (req.serialCount() < 0 || req.serialCount() > 5000))
            throw new IllegalArgumentException("일련번호 개수 상한 초과(≤5000)");
        if (req.elementsJson() != null && req.elementsJson().length() > 100_000)
            throw new IllegalArgumentException("라벨 정의가 너무 큽니다");
        String varsJson = req.variables() != null ? mapper.writeValueAsString(req.variables()) : "{}";
        return fleet.enqueuePrint(orgId, id, req.widthMm(), req.heightMm(), req.dpi(),
                req.elementsJson(), varsJson, req.copies() != null ? req.copies() : 1,
                req.seqVar(), req.serialPrefix(), req.serialStart(), req.serialCount(), req.serialPad());
    }

    public record NetworkPrintRequest(double widthMm, double heightMm, Integer dpi,
                                      String elementsJson, Map<String, String> variables, Integer copies,
                                      String seqVar, String serialPrefix, Integer serialStart,
                                      Integer serialCount, Integer serialPad) {}
}
