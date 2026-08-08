package com.printscan.cloud.web;

import com.printscan.cloud.HubProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

/** SaaS admin 게이트: token 설정 시 /api/admin 에 헤더 요구, 미설정 시 개방(온프렘). */
class AdminAuthFilterTest {

    /** protected doFilterInternal 노출용. */
    static class Testable extends AdminAuthFilter {
        Testable(HubProperties h) { super(h); }
        void call(HttpServletRequest r, HttpServletResponse s, FilterChain c) throws Exception {
            doFilterInternal(r, s, c);
        }
    }

    private HttpServletRequest req(String uri, String token) {
        HttpServletRequest r = mock(HttpServletRequest.class);
        when(r.getRequestURI()).thenReturn(uri);
        when(r.getHeader("X-Admin-Token")).thenReturn(token);
        return r;
    }

    @Test
    void 토큰설정_헤더없으면_401() throws Exception {
        HubProperties h = new HubProperties(); h.setAdminToken("SECRET");
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        new Testable(h).call(req("/api/admin/stats", null), res, chain);
        verify(res).sendError(401, "admin token required");
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void 토큰설정_올바른헤더면_통과() throws Exception {
        HubProperties h = new HubProperties(); h.setAdminToken("SECRET");
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        new Testable(h).call(req("/api/admin/stats", "SECRET"), res, chain);
        verify(chain).doFilter(any(), any());
        verify(res, never()).sendError(anyInt(), anyString());
    }

    @Test
    void 토큰미설정_온프렘_개방() throws Exception {
        HubProperties h = new HubProperties(); // adminToken="" 기본
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        new Testable(h).call(req("/api/admin/stats", null), res, chain);
        verify(chain).doFilter(any(), any());
    }

    @Test
    void admin외_경로는_토큰무관_통과() throws Exception {
        HubProperties h = new HubProperties(); h.setAdminToken("SECRET");
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        new Testable(h).call(req("/api/device/register", null), res, chain);
        verify(chain).doFilter(any(), any());
    }
}
