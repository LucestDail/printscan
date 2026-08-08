package com.printscan.cloud.web;

import com.printscan.cloud.domain.Organization;
import com.printscan.cloud.domain.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 관리자 요청의 테넌트(org) 확정.
 * - X-Org-Key(=Organization.apiKey) 있으면 그 조직.
 * - 없고 조직이 정확히 1개면 그 조직(온프렘 단일테넌트 폴백 — LAN 데모 무중단).
 * - 그 외(멀티테넌트인데 키 없음) → 거부.
 */
@Component
@RequiredArgsConstructor
public class OrgContext {

    private final OrganizationRepository orgs;

    @Transactional(readOnly = true)
    public Organization resolve(String orgKey) {
        if (orgKey != null && !orgKey.isBlank()) {
            return orgs.findByApiKey(orgKey)
                    .orElseThrow(() -> new IllegalArgumentException("잘못된 X-Org-Key"));
        }
        List<Organization> all = orgs.findAll();
        if (all.size() == 1) return all.get(0);
        throw new IllegalArgumentException("멀티테넌트 환경: X-Org-Key 헤더가 필요합니다.");
    }
}
