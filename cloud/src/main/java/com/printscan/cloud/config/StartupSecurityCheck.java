package com.printscan.cloud.config;

import com.printscan.cloud.CloudProperties;
import com.printscan.cloud.HubProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 기동 시 데모/무방비 설정 경고 — 리세일/SaaS 배포 시 기본값 방치 방지.
 * warnings() 분리로 테스트 가능. 비파괴(로그만).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StartupSecurityCheck implements ApplicationRunner {

    private final CloudProperties cloud;
    private final HubProperties hub;

    public List<String> warnings() {
        List<String> w = new ArrayList<>();
        if ("ORG-DEMO-KEY".equals(cloud.getBootstrapApiKey())) {
            w.add("데모 org-key(ORG-DEMO-KEY) 사용 중 — 실배포 시 조직 키를 로테이션하세요(POST /api/admin/org/rotate-key).");
        }
        boolean saas = "saas".equalsIgnoreCase(hub.getMode());
        boolean noAdminToken = hub.getAdminToken() == null || hub.getAdminToken().isBlank();
        if (saas && noAdminToken) {
            w.add("SaaS 모드인데 admin-token 미설정 — /api/admin/** 이 무방비입니다. PRINTSCAN_HUB_ADMIN_TOKEN 을 설정하세요.");
        }
        return w;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> w = warnings();
        if (!w.isEmpty()) {
            log.warn("================ 보안 점검(cloud) ================");
            w.forEach(m -> log.warn("⚠️  {}", m));
            log.warn("=================================================");
        }
    }
}
