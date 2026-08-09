package com.printscan.cloud.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.printscan.cloud.domain.*;
import com.printscan.cloud.service.FleetService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 관리자 UI — 테넌트는 서버 세션(로그인)으로 확정(OrgContext), 없으면 X-Org-Key 헤더/단일org 폴백.
 * 조회/집계/네트워크출력/템플릿 모두 호출자 org 로 한정 → 테넌트 격리.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminApiController {

    private final FleetService fleet;
    private final OrgContext orgContext;
    private final ObjectMapper mapper = new ObjectMapper();

    private Long org(HttpServletRequest req) { return orgContext.resolve(req).getId(); }

    @GetMapping("/devices")
    public List<Device> devices(HttpServletRequest req) { return fleet.allDevices(org(req)); }

    @GetMapping("/jobs")
    public List<PrintJobCloud> jobs(HttpServletRequest req) { return fleet.recentJobs(org(req)); }

    @GetMapping("/snapshots")
    public List<InventorySnapshot> snapshots(HttpServletRequest req) { return fleet.allSnapshots(org(req)); }

    @GetMapping("/stats")
    public Map<String, Object> stats(HttpServletRequest req) { return fleet.stats(org(req)); }

    @GetMapping("/consumption")
    public Map<String, Object> consumption(HttpServletRequest req) { return fleet.consumption(org(req)); }

    @GetMapping("/templates")
    public List<CloudTemplate> templates(HttpServletRequest req) { return fleet.listTemplates(org(req)); }

    @PostMapping("/templates")
    public CloudTemplate saveTemplate(HttpServletRequest req, @RequestBody CloudTemplate t) {
        return fleet.saveTemplate(org(req), t);
    }

    @DeleteMapping("/templates/{id}")
    public ResponseEntity<Void> deleteTemplate(HttpServletRequest req, @PathVariable Long id) {
        fleet.deleteTemplate(org(req), id);
        return ResponseEntity.noContent().build();
    }

    /** 네트워크 출력 지시 — 대상 장비가 호출자 org 소속이어야 함(격리). */
    @PostMapping("/devices/{id}/print")
    public PrintJobCloud print(HttpServletRequest req, @PathVariable Long id, @RequestBody NetworkPrintRequest r) throws Exception {
        Long orgId = org(req);
        if (r.copies() != null && r.copies() > 1000) throw new ApiException("error.copiesMax", 1000);
        if (r.serialCount() != null && (r.serialCount() < 0 || r.serialCount() > 5000))
            throw new ApiException("error.serialMax", 5000);
        if (r.elementsJson() != null && r.elementsJson().length() > 100_000)
            throw new ApiException("error.elementsTooBig");
        String varsJson = r.variables() != null ? mapper.writeValueAsString(r.variables()) : "{}";
        return fleet.enqueuePrint(orgId, id, r.widthMm(), r.heightMm(), r.dpi(),
                r.elementsJson(), varsJson, r.copies() != null ? r.copies() : 1,
                r.seqVar(), r.serialPrefix(), r.serialStart(), r.serialCount(), r.serialPad());
    }

    public record NetworkPrintRequest(double widthMm, double heightMm, Integer dpi,
                                      String elementsJson, Map<String, String> variables, Integer copies,
                                      String seqVar, String serialPrefix, Integer serialStart,
                                      Integer serialCount, Integer serialPad) {}

    // ── org-key 로테이션(테넌트 셀프서비스, 호출자 org 로 한정) ──

    /** 현재 org-key + 로테이션 상태(직전 키 유효 여부/만료 시각). 키 유출 대비 UI 표시용. */
    @GetMapping("/org/key")
    public Map<String, Object> orgKey(HttpServletRequest req) {
        Organization o = orgContext.resolve(req);
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("apiKey", o.getApiKey());
        m.put("previousKeyExpiresAt", o.getPreviousKeyExpiresAt());
        m.put("keyRotatedAt", o.getKeyRotatedAt());
        boolean graceActive = o.getPreviousApiKey() != null && o.getPreviousKeyExpiresAt() != null
                && o.getPreviousKeyExpiresAt().isAfter(java.time.LocalDateTime.now());
        m.put("previousKeyActive", graceActive);
        return m;
    }

    /** 키 로테이션 — 신규 키 발급. graceMinutes(기본 60, 최대 7일) 동안 직전 키도 유효. 0=즉시 폐기. */
    @PostMapping("/org/rotate-key")
    public Map<String, Object> rotateKey(HttpServletRequest req, @RequestBody(required = false) Map<String, Object> body) {
        Long orgId = org(req);
        long grace = 60;
        if (body != null && body.get("graceMinutes") != null) grace = ((Number) body.get("graceMinutes")).longValue();
        return fleet.rotateOrgKey(orgId, grace);
    }

    /** 직전 키 즉시 폐기(유출 대응). */
    @PostMapping("/org/revoke-previous-key")
    public ResponseEntity<Void> revokePrevious(HttpServletRequest req) {
        fleet.revokePreviousKey(org(req));
        return ResponseEntity.noContent().build();
    }
}
