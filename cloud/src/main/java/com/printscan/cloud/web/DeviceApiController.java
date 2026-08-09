package com.printscan.cloud.web;

import com.printscan.cloud.domain.Device;
import com.printscan.cloud.domain.PrintJobCloud;
import com.printscan.cloud.service.FleetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 디바이스(온디바이스 edge)가 아웃바운드로 호출하는 엔드포인트. */
@RestController
@RequestMapping("/api/device")
@RequiredArgsConstructor
public class DeviceApiController {

    private final FleetService fleet;
    private static final String H = "X-Device-Token";

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        // 예외는 GlobalExceptionHandler 가 요청 로케일로 번역(자체 catch 금지).
        Device d = fleet.register(body.get("orgApiKey"), body.get("name"), body.get("printerMode"), body.get("line"));
        return ResponseEntity.ok(Map.of("deviceId", d.getId(), "deviceToken", d.getDeviceToken()));
    }

    /** 다음 인쇄 잡 폴링. 없으면 204. */
    @GetMapping("/jobs/next")
    public ResponseEntity<?> next(@RequestHeader(H) String token) {
        Device d = fleet.authDevice(token);
        PrintJobCloud job = fleet.pollNext(d);
        return job == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(job);
    }

    @PostMapping("/jobs/{id}/ack")
    public ResponseEntity<Void> ack(@RequestHeader(H) String token, @PathVariable Long id,
                                    @RequestBody Map<String, Object> body) {
        Device d = fleet.authDevice(token);
        fleet.ack(d, id, Boolean.TRUE.equals(body.get("ok")), String.valueOf(body.getOrDefault("message", "")));
        return ResponseEntity.ok().build();
    }

    /** 디바이스가 자기 org 의 중앙 템플릿을 폴링으로 받아 로컬 동기화. */
    @GetMapping("/templates")
    public java.util.List<com.printscan.cloud.domain.CloudTemplate> templates(@RequestHeader(H) String token) {
        return fleet.templatesForDevice(fleet.authDevice(token));
    }

    @PostMapping("/heartbeat")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Void> heartbeat(@RequestHeader(H) String token, @RequestBody Map<String, Object> body) {
        Device d = fleet.authDevice(token);
        fleet.heartbeat(d, (String) body.get("printerMode"), (String) body.get("line"),
                (List<Map<String, Object>>) body.get("inventory"));
        return ResponseEntity.ok().build();
    }

    /** 소비(출고) 업싱크 — 인쇄 자동출고/수동출고 이벤트. */
    @PostMapping("/consume")
    public ResponseEntity<Void> consume(@RequestHeader(H) String token, @RequestBody Map<String, Object> body) {
        Device d = fleet.authDevice(token);
        int qty = body.get("qty") == null ? 0 : ((Number) body.get("qty")).intValue();
        fleet.recordConsumption(d, (String) body.get("code"), qty,
                (String) body.get("operator"), (String) body.get("line"),
                Boolean.TRUE.equals(body.get("fromPrint")));
        return ResponseEntity.ok().build();
    }
}
