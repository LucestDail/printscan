package com.printscan.cloud.service;

import com.printscan.cloud.CloudProperties;
import com.printscan.cloud.domain.Organization;
import com.printscan.cloud.domain.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;

/** 데모 부트스트랩: 기본 조직(고정 apiKey) 없으면 생성 → 디바이스가 바로 등록 가능. */
@Slf4j
@Component
@RequiredArgsConstructor
public class Bootstrap implements ApplicationRunner {

    private final OrganizationRepository orgs;
    private final CloudProperties props;

    @Override
    public void run(ApplicationArguments args) {
        orgs.findByApiKey(props.getBootstrapApiKey()).orElseGet(() -> {
            Organization o = new Organization();
            o.setName(props.getBootstrapOrg());
            o.setApiKey(props.getBootstrapApiKey());
            log.info("[cloud] 기본 조직 생성: {} (apiKey={})", o.getName(), o.getApiKey());
            return orgs.save(o);
        });
    }
}
