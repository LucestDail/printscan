package com.printscan.cloud.web;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** IP 고정창 레이트리밋: 보호 경로만 제한, 임계 초과 시 429. */
class RateLimitFilterTest {

    private final RateLimitFilter filter = new RateLimitFilter(3); // 분당 3회

    private final FilterChain chain = new FilterChain() {
        final AtomicInteger passed = new AtomicInteger();
        @Override public void doFilter(jakarta.servlet.ServletRequest r, jakarta.servlet.ServletResponse s) {
            passed.incrementAndGet();
        }
    };

    private MockHttpServletResponse fire(String ip, String uri) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", uri);
        req.setRemoteAddr(ip); // IP 별 버킷 → 테스트 간 격리
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, chain); // OncePerRequestFilter → doFilterInternal
        return res;
    }

    @Test
    void 보호경로_임계초과시_429() throws Exception {
        assertEquals(200, fire("10.0.0.1", "/api/device/register").getStatus());
        assertEquals(200, fire("10.0.0.1", "/api/device/register").getStatus());
        assertEquals(200, fire("10.0.0.1", "/api/device/register").getStatus());
        assertEquals(429, fire("10.0.0.1", "/api/device/register").getStatus(), "4번째는 제한(429)");
    }

    @Test
    void 비보호경로는_제한없음() throws Exception {
        for (int i = 0; i < 10; i++) {
            assertEquals(200, fire("10.0.0.2", "/api/device/jobs/next").getStatus(), "비보호 경로는 무제한 통과");
        }
    }

    @Test
    void admin경로도_보호() throws Exception {
        assertEquals(200, fire("10.0.0.3", "/api/admin/templates").getStatus());
        assertEquals(200, fire("10.0.0.3", "/api/admin/templates").getStatus());
        assertEquals(200, fire("10.0.0.3", "/api/admin/templates").getStatus());
        assertEquals(429, fire("10.0.0.3", "/api/admin/templates").getStatus());
    }
}
