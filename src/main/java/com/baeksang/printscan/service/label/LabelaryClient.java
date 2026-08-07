package com.baeksang.printscan.service.label;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Labelary API 로 ZPL → PNG 렌더(라벨 디자이너 실시간 미리보기용). 키 불필요.
 * dpi 203 → 8dpmm, 300 → 12dpmm. 라벨 크기는 inch(=mm/25.4).
 */
@Slf4j
@Component
public class LabelaryClient {

    private static final String BASE = "http://api.labelary.com/v1/printers/";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    /** ZPL 을 PNG 바이트로 렌더. */
    public byte[] previewPng(String zpl, int dpi, int widthMm, int heightMm) throws Exception {
        int dpmm = dpi >= 300 ? 12 : 8;
        double wIn = Math.max(0.5, widthMm / 25.4);
        double hIn = Math.max(0.5, heightMm / 25.4);
        String url = String.format("%s%ddpmm/labels/%.2fx%.2f/0/", BASE, dpmm, wIn, hIn);

        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(12))
                .header("Accept", "image/png")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(zpl, StandardCharsets.UTF_8))
                .build();

        HttpResponse<byte[]> res = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
        if (res.statusCode() != 200) {
            String err = new String(res.body(), StandardCharsets.UTF_8);
            throw new IllegalStateException("Labelary 오류 " + res.statusCode() + ": " + err);
        }
        log.info("[labelary] preview {}dpmm {}x{}mm ({} bytes)", dpmm, widthMm, heightMm, res.body().length);
        return res.body();
    }
}
