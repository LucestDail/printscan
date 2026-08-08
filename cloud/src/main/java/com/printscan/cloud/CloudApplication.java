package com.printscan.cloud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * printscan v2 — Cloud SaaS. 멀티테넌트 플릿 관리 + 출력 집계 + 네트워크 출력 디스패치.
 * 디바이스는 아웃바운드 HTTP 로 등록·폴링(방화벽 친화). 인쇄지시는 잡 큐 → 디바이스 폴링 수령.
 */
@SpringBootApplication
@EnableScheduling
public class CloudApplication {
    public static void main(String[] args) {
        SpringApplication.run(CloudApplication.class, args);
    }
}
