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
        try {
            Device d = fleet.register(body.get("orgApiKey"), body.get("name"), body.get("printerMode"));
            return ResponseEntity.ok(Map.of("deviceId", d.getId(), "deviceToken", d.getDeviceToken()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
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

    @PostMapping("/heartbeat")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Void> heartbeat(@RequestHeader(H) String token, @RequestBody Map<String, Object> body) {
        Device d = fleet.authDevice(token);
        fleet.heartbeat(d, (String) body.get("printerMode"), (List<Map<String, Object>>) body.get("inventory"));
        return ResponseEntity.ok().build();
    }
}
