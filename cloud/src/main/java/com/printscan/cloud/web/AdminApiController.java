package com.printscan.cloud.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.printscan.cloud.domain.*;
import com.printscan.cloud.service.FleetService;
import lombok.RequiredArgsConstructor;
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

    /** 네트워크 출력 지시 — 대상 장비가 호출자 org 소속이어야 함(격리). */
    @PostMapping("/devices/{id}/print")
    public PrintJobCloud print(@RequestHeader(value = ORG, required = false) String k,
                               @PathVariable Long id, @RequestBody NetworkPrintRequest req) throws Exception {
        Long orgId = orgContext.resolve(k).getId();
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
