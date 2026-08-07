package com.printscan.edge.cloud;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** 클라우드 동기화 설정. enabled=true 면 등록·폴링·하트비트 수행. */
@Getter
@Setter
@ConfigurationProperties(prefix = "printscan.cloud")
public class CloudSyncProperties {
    private boolean enabled = false;
    private String baseUrl = "http://localhost:8092";
    private String orgApiKey = "ORG-DEMO-KEY";
    private String deviceName = "edge-device";
    private long pollMs = 2000;
    private long heartbeatMs = 15000;
}
