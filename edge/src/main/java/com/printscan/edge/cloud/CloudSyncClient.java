package com.printscan.edge.cloud;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.printscan.edge.config.PrinterProperties;
import com.printscan.edge.inventory.InventoryService;
import com.printscan.edge.inventory.Product;
import com.printscan.edge.label.LabelService;
import com.printscan.edge.label.RenderRequest;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 클라우드 동기화(아웃바운드): 등록 → 잡 폴링(로컬 래스터 인쇄) → ack + 하트비트(재고 업싱크).
 * 방화벽 친화(디바이스가 클라우드로 나가기만 함). 실패는 조용히 재시도.
 */
@Slf4j
@Component
public class CloudSyncClient {

    private final CloudSyncProperties props;
    private final DeviceIdentityRepository identityRepo;
    private final LabelService labelService;
    private final InventoryService inventory;
    private final PrinterProperties printer;
    private final ObjectMapper mapper = new ObjectMapper();
    private final RestClient http;

    private volatile String token;
    private volatile Long deviceId;

    public CloudSyncClient(CloudSyncProperties props, DeviceIdentityRepository identityRepo,
                           LabelService labelService, InventoryService inventory, PrinterProperties printer) {
        this.props = props;
        this.identityRepo = identityRepo;
        this.labelService = labelService;
        this.inventory = inventory;
        this.printer = printer;
        this.http = RestClient.builder().baseUrl(props.getBaseUrl()).build();
    }

    @PostConstruct
    void init() {
        if (!props.isEnabled()) { log.info("[cloud-sync] 비활성"); return; }
        identityRepo.findById(1L).ifPresent(d -> { token = d.getDeviceToken(); deviceId = d.getCloudDeviceId(); });
        if (token == null) register();
        log.info("[cloud-sync] 활성 baseUrl={} deviceId={}", props.getBaseUrl(), deviceId);
    }

    private void register() {
        try {
            Map<?, ?> res = http.post().uri("/api/device/register")
                    .body(Map.of("orgApiKey", props.getOrgApiKey(), "name", props.getDeviceName(),
                            "printerMode", printer.getMode()))
                    .retrieve().body(Map.class);
            if (res != null && res.get("deviceToken") != null) {
                token = String.valueOf(res.get("deviceToken"));
                deviceId = ((Number) res.get("deviceId")).longValue();
                DeviceIdentity d = new DeviceIdentity();
                d.setId(1L); d.setCloudDeviceId(deviceId); d.setDeviceToken(token);
                identityRepo.save(d);
                log.info("[cloud-sync] 등록 완료 deviceId={}", deviceId);
            }
        } catch (Exception e) {
            log.warn("[cloud-sync] 등록 실패(재시도 예정): {}", e.getMessage());
        }
    }

    /** 인쇄 잡 폴링 → 로컬 래스터 인쇄 → ack. */
    @Scheduled(fixedDelayString = "${printscan.cloud.poll-ms:2000}")
    public void pollJobs() {
        if (!props.isEnabled() || token == null) return;
        try {
            Map<?, ?> job = http.get().uri("/api/device/jobs/next")
                    .header("X-Device-Token", token).retrieve().body(Map.class);
            if (job == null || job.get("id") == null) return;
            Long jobId = ((Number) job.get("id")).longValue();
            boolean ok = true; String msg = "printed";
            try {
                RenderRequest req = new RenderRequest(
                        null, "cloud-job",
                        num(job.get("widthMm")), num(job.get("heightMm")),
                        job.get("dpi") != null ? ((Number) job.get("dpi")).intValue() : null,
                        (String) job.get("elementsJson"),
                        parseVars((String) job.get("variablesJson")),
                        job.get("copies") != null ? ((Number) job.get("copies")).intValue() : 1);
                labelService.print(req);
                log.info("[cloud-sync] 네트워크 출력 잡 {} 인쇄 완료", jobId);
            } catch (Exception e) {
                ok = false; msg = e.getMessage();
                log.warn("[cloud-sync] 잡 {} 인쇄 실패: {}", jobId, msg);
            }
            http.post().uri("/api/device/jobs/{id}/ack", jobId)
                    .header("X-Device-Token", token)
                    .body(Map.of("ok", ok, "message", msg == null ? "" : msg))
                    .retrieve().toBodilessEntity();
        } catch (Exception e) {
            log.debug("[cloud-sync] 폴링 스킵: {}", e.getMessage());
        }
    }

    /** 하트비트 + 재고 업싱크. */
    @Scheduled(fixedDelayString = "${printscan.cloud.heartbeat-ms:15000}")
    public void heartbeat() {
        if (!props.isEnabled() || token == null) return;
        try {
            List<Map<String, Object>> inv = inventory.findAll().stream().map(this::snap).collect(Collectors.toList());
            http.post().uri("/api/device/heartbeat")
                    .header("X-Device-Token", token)
                    .body(Map.of("printerMode", printer.getMode(), "inventory", inv))
                    .retrieve().toBodilessEntity();
        } catch (Exception e) {
            log.debug("[cloud-sync] 하트비트 스킵: {}", e.getMessage());
        }
    }

    private Map<String, Object> snap(Product p) {
        return Map.of("code", p.getCode(), "name", p.getName(), "quantity", p.getQuantity());
    }

    private Map<String, String> parseVars(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try { return mapper.readValue(json, Map.class); } catch (Exception e) { return Map.of(); }
    }

    private double num(Object o) { return o == null ? 0 : ((Number) o).doubleValue(); }
}
