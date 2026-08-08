package com.printscan.edge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * printscan v2 — On-Device Edge 앱 (라즈베리파이 어플라이언스).
 * 단일 프로세스에서 UI(서버렌더) + 라벨 래스터 렌더(^GFA) + 로컬 인쇄 + 클라우드 동기화(폴링).
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
@EnableAsync
public class EdgeApplication {
    public static void main(String[] args) {
        SpringApplication.run(EdgeApplication.class, args);
    }
}
