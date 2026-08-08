package com.printscan.cloud.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 허브 운영 알림 — WARN 로그 + (webhook 설정 시) POST. key 로 5분 중복 억제. */
@Slf4j
@Service
public class AlertService {

    private final String webhook;
    private final RestClient http;
    private final ConcurrentHashMap<String, Long> lastSent = new ConcurrentHashMap<>();

    public AlertService(@Value("${printscan.alert.webhook:}") String webhook) {
        this.webhook = webhook;
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout((int) Duration.ofSeconds(2).toMillis());
        rf.setReadTimeout((int) Duration.ofSeconds(3).toMillis());
        this.http = RestClient.builder().requestFactory(rf).build();
    }

    public void alert(String level, String key, String message) {
        long now = System.currentTimeMillis();
        Long prev = lastSent.get(key);
        if (prev != null && now - prev < 300_000) return;
        lastSent.put(key, now);
        log.warn("[alert] {} {} — {}", level, key, message);
        if (webhook == null || webhook.isBlank()) return;
        try {
            http.post().uri(webhook)
                    .body(Map.of("level", level, "key", key, "message", message, "source", "printscan-hub"))
                    .retrieve().toBodilessEntity();
        } catch (Exception e) {
            log.debug("[alert] webhook 실패: {}", e.getMessage());
        }
    }
}
