package com.printscan.cloud.web;

import com.printscan.cloud.HubProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * SaaS 보호: printscan.hub.admin-token 이 설정된 경우에만 /api/admin/** 에 X-Admin-Token 요구.
 * 온프렘(토큰 미설정)에서는 개방 — 결정사항 "LAN 경량 + SaaS만 강화" 반영.
 */
@Component
@RequiredArgsConstructor
public class AdminAuthFilter extends OncePerRequestFilter {

    private final HubProperties hub;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String token = hub.getAdminToken();
        if (token != null && !token.isBlank() && req.getRequestURI().startsWith("/api/admin/")) {
            if (!token.equals(req.getHeader("X-Admin-Token"))) {
                res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "admin token required");
                return;
            }
        }
        chain.doFilter(req, res);
    }
}
