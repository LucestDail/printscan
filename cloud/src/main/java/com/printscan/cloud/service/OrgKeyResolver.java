package com.printscan.cloud.service;

import com.printscan.cloud.domain.Organization;
import com.printscan.cloud.domain.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * org-key → 조직 확정(로테이션 인지). 현재 키가 우선, 없으면 유예기간 내 직전 키 허용.
 * 로그인/헤더/디바이스 등록 모두 이 한 곳으로 통일해 로테이션이 전 경로에 일관 적용.
 */
@Component
@RequiredArgsConstructor
public class OrgKeyResolver {

    private final OrganizationRepository orgs;

    @Transactional(readOnly = true)
    public Optional<Organization> resolve(String key) {
        if (key == null || key.isBlank()) return Optional.empty();
        Optional<Organization> current = orgs.findByApiKey(key);
        if (current.isPresent()) return current;
        // 직전 키: 만료 시각이 설정되어 있고 아직 지나지 않았을 때만 유효
        return orgs.findByPreviousApiKey(key)
                .filter(o -> o.getPreviousKeyExpiresAt() != null
                        && o.getPreviousKeyExpiresAt().isAfter(LocalDateTime.now()));
    }
}
