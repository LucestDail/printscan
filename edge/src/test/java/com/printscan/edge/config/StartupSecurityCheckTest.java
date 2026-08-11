package com.printscan.edge.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 기본 자격증명 경고 로직. */
class StartupSecurityCheckTest {

    private StartupSecurityCheck check(String password) {
        SecurityConfig.SecurityProperties p = new SecurityConfig.SecurityProperties();
        p.setPassword(password);
        return new StartupSecurityCheck(p);
    }

    @Test
    void 기본_비밀번호면_경고() {
        assertEquals(1, check("printscan").warnings().size());
    }

    @Test
    void 교체된_비밀번호면_경고없음() {
        assertTrue(check("Str0ng!Pass").warnings().isEmpty());
    }
}
