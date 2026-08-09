package com.printscan.cloud.web;

import com.printscan.cloud.domain.Organization;
import com.printscan.cloud.domain.OrganizationRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
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

    public static final String SESSION_ORG = "orgId";

    /**
     * 요청의 테넌트 확정: ①서버 세션(로그인) ②X-Org-Key 헤더(프로그램 접근) ③단일 org 폴백(온프렘).
     * 세션 우선 → localStorage/헤더 노출 없이 쿠키 기반.
     */
    @Transactional(readOnly = true)
    public Organization resolve(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        if (s != null && s.getAttribute(SESSION_ORG) != null) {
            Long id = (Long) s.getAttribute(SESSION_ORG);
            return orgs.findById(id).orElseThrow(() -> new ApiException("error.session"));
        }
        return resolve(req.getHeader("X-Org-Key"));
    }

    @Transactional(readOnly = true)
    public Organization resolve(String orgKey) {
        if (orgKey != null && !orgKey.isBlank()) {
            return orgs.findByApiKey(orgKey).orElseThrow(() -> new ApiException("error.badOrgKey"));
        }
        List<Organization> all = orgs.findAll();
        if (all.size() == 1) return all.get(0);
        throw new ApiException("error.orgKeyRequired");
    }
}
