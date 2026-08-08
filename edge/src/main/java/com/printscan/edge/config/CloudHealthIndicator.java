package com.printscan.edge.config;

import com.printscan.edge.cloud.CloudSyncClient;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/** /actuator/health 의 'cloud' 구성요소 — 허브 동기화 연결 상태. 비활성이면 UP(n/a). */
@Component("cloud")
@RequiredArgsConstructor
public class CloudHealthIndicator implements HealthIndicator {

    private final CloudSyncClient sync;

    @Override
    public Health health() {
        if (!sync.isEnabled()) return Health.up().withDetail("sync", "disabled").build();
        long last = sync.lastContactMs();
        long ageMs = last == 0 ? -1 : System.currentTimeMillis() - last;
        // 폴 주기 대비 넉넉히 60초 이내면 정상
        if (last != 0 && ageMs < 60_000) {
            return Health.up().withDetail("lastContactMs", ageMs).build();
        }
        return Health.down().withDetail("reason", last == 0 ? "허브 접촉 이력 없음" : "허브 미접촉 " + ageMs + "ms").build();
    }
}
