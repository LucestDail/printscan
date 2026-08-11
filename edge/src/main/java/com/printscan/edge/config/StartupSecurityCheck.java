package com.printscan.edge.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 기동 시 기본 자격증명 사용을 경고 — 리세일 배포 시 기본값 방치 방지.
 * warnings() 를 분리해 테스트 가능. 비파괴(로그만) — 배포 편의를 위해 기동은 막지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StartupSecurityCheck implements ApplicationRunner {

    private final SecurityConfig.SecurityProperties security;

    /** 위험 항목 메시지 목록(없으면 빈 목록). */
    public List<String> warnings() {
        List<String> w = new ArrayList<>();
        if ("printscan".equals(security.getPassword())) {
            w.add("기본 비밀번호(admin/printscan) 사용 중 — 배포 시 PRINTSCAN_SECURITY_PASSWORD 로 반드시 교체하세요.");
        }
        return w;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> w = warnings();
        if (!w.isEmpty()) {
            log.warn("================ 보안 점검(edge) ================");
            w.forEach(m -> log.warn("⚠️  {}", m));
            log.warn("================================================");
        }
    }
}
