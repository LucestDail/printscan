package com.printscan.cloud.web;

import com.printscan.cloud.domain.Organization;
import com.printscan.cloud.domain.OrganizationRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 서버 세션 로그인 — org-key 를 HttpSession(orgId)으로 승격. localStorage/헤더 노출 제거. */
@RestController
@RequiredArgsConstructor
public class LoginController {

    private final OrganizationRepository orgs;

    @PostMapping("/api/login")
    public ResponseEntity<?> login(@RequestBody(required = false) Map<String, String> body, HttpServletRequest req) {
        String key = body == null ? null : body.get("orgKey");
        Organization org;
        if (key == null || key.isBlank()) {
            List<Organization> all = orgs.findAll();       // 온프렘 단일 org 폴백
            if (all.size() != 1) return ResponseEntity.status(401).body(Map.of("error", "조직 키가 필요합니다."));
            org = all.get(0);
        } else {
            org = orgs.findByApiKey(key).orElse(null);
            if (org == null) return ResponseEntity.status(401).body(Map.of("error", "잘못된 조직 키"));
        }
        req.getSession(true).setAttribute(OrgContext.SESSION_ORG, org.getId());
        return ResponseEntity.ok(Map.of("org", org.getName()));
    }

    @PostMapping("/api/logout")
    public ResponseEntity<Void> logout(HttpServletRequest req) {
        if (req.getSession(false) != null) req.getSession(false).invalidate();
        return ResponseEntity.ok().build();
    }
}
