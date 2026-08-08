package com.printscan.cloud.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 간이 IP 고정창 레이트리밋 — /api/device/register(등록 스팸/키 브루트포스)와 /api/admin(테넌트 브루트포스) 보호.
 * 분산 환경엔 부적합(인스턴스별) — 문서상 리버스프록시/게이트웨이 레이트리밋 권장. 최소 방어선.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final int perMinute;
    private final ConcurrentHashMap<String, long[]> buckets = new ConcurrentHashMap<>(); // ip → [windowStartMs, count]

    public RateLimitFilter(@Value("${printscan.ratelimit.per-minute:120}") int perMinute) {
        this.perMinute = perMinute;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String uri = req.getRequestURI();
        if (uri.startsWith("/api/device/register") || uri.startsWith("/api/admin/")) {
            String ip = req.getRemoteAddr();
            long now = System.currentTimeMillis();
            long[] b = buckets.compute(ip, (k, v) -> {
                if (v == null || now - v[0] > 60_000) return new long[]{now, 1};
                v[1]++;
                return v;
            });
            if (b[1] > perMinute) {
                res.setStatus(429);
                res.getWriter().write("{\"error\":\"rate limit exceeded\"}");
                return;
            }
        }
        chain.doFilter(req, res);
    }
}
