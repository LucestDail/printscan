package com.printscan.cloud.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.printscan.cloud.domain.*;
import com.printscan.cloud.service.FleetService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 관리자 UI 가 호출 — 플릿 조회 + 네트워크 출력 지시. (데모: 인증 생략, 실서비스는 org 세션 필요) */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminApiController {

    private final FleetService fleet;
    private final ObjectMapper mapper = new ObjectMapper();

    @GetMapping("/devices")
    public List<Device> devices() { return fleet.allDevices(); }

    @GetMapping("/jobs")
    public List<PrintJobCloud> jobs() { return fleet.recentJobs(); }

    @GetMapping("/snapshots")
    public List<InventorySnapshot> snapshots() { return fleet.allSnapshots(); }

    @GetMapping("/stats")
    public Map<String, Object> stats() { return fleet.stats(); }

    @GetMapping("/consumption")
    public Map<String, Object> consumption() { return fleet.consumption(); }

    /** 네트워크 출력 지시: 특정 디바이스에 라벨 잡 큐잉. */
    @PostMapping("/devices/{id}/print")
    public PrintJobCloud print(@PathVariable Long id, @RequestBody NetworkPrintRequest req) throws Exception {
        String varsJson = req.variables() != null ? mapper.writeValueAsString(req.variables()) : "{}";
        return fleet.enqueuePrint(id, req.widthMm(), req.heightMm(), req.dpi(),
                req.elementsJson(), varsJson, req.copies() != null ? req.copies() : 1,
                req.seqVar(), req.serialPrefix(), req.serialStart(), req.serialCount(), req.serialPad());
    }

    public record NetworkPrintRequest(double widthMm, double heightMm, Integer dpi,
                                      String elementsJson, Map<String, String> variables, Integer copies,
                                      String seqVar, String serialPrefix, Integer serialStart,
                                      Integer serialCount, Integer serialPad) {}
}
