package com.printscan.cloud.config;

import com.printscan.cloud.CloudProperties;
import com.printscan.cloud.HubProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 데모 org-key / SaaS admin-token 미설정 경고 로직. */
class StartupSecurityCheckTest {

    private CloudProperties cloud(String key) {
        CloudProperties c = new CloudProperties();
        c.setBootstrapApiKey(key);
        return c;
    }

    private HubProperties hub(String mode, String token) {
        HubProperties h = new HubProperties();
        h.setMode(mode);
        h.setAdminToken(token);
        return h;
    }

    @Test
    void 데모키_사용시_경고() {
        var w = new StartupSecurityCheck(cloud("ORG-DEMO-KEY"), hub("onprem", "")).warnings();
        assertEquals(1, w.size());
    }

    @Test
    void 실키_온프렘_경고없음() {
        var w = new StartupSecurityCheck(cloud("ORG-REAL-123"), hub("onprem", "")).warnings();
        assertTrue(w.isEmpty());
    }

    @Test
    void SaaS_admin토큰_미설정시_경고() {
        var w = new StartupSecurityCheck(cloud("ORG-REAL-123"), hub("saas", "")).warnings();
        assertEquals(1, w.size());
    }

    @Test
    void SaaS_admin토큰_설정시_경고없음() {
        var w = new StartupSecurityCheck(cloud("ORG-REAL-123"), hub("saas", "strong-token")).warnings();
        assertTrue(w.isEmpty());
    }

    @Test
    void 데모키_and_SaaS무토큰_둘다_경고() {
        var w = new StartupSecurityCheck(cloud("ORG-DEMO-KEY"), hub("saas", "")).warnings();
        assertEquals(2, w.size());
    }
}
