package com.printscan.cloud.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 허브 HTTP 통합 — 디바이스 등록·원격 인쇄 왕복(QUEUED→SENT→DONE)·입력 상한·관리 폴백.
 * 감사 지적("컨트롤러/통합 0% 커버리지") 해소. 전체 컨텍스트 부팅(부트스트랩 조직 포함).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:cloudit;DB_CLOSE_DELAY=-1",
        "spring.flyway.enabled=false",
        "printscan.cloud.bootstrap-api-key=ORG-DEMO-KEY"
})
class CloudApiIntegrationTest {

    @Autowired TestRestTemplate rest;

    private HttpEntity<String> json(String body) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, h);
    }

    private HttpEntity<String> jsonWithToken(String body, String token) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("X-Device-Token", token);
        return new HttpEntity<>(body, h);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> register(String key) {
        ResponseEntity<Map> r = rest.postForEntity("/api/device/register",
                json("{\"orgApiKey\":\"" + key + "\",\"name\":\"dev\",\"printerMode\":\"cups\",\"line\":\"L1\"}"), Map.class);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        return r.getBody();
    }

    @Test
    void 디바이스_등록_및_잘못된키_400() {
        Map<String, Object> reg = register("ORG-DEMO-KEY");
        assertNotNull(reg.get("deviceToken"));
        assertNotNull(reg.get("deviceId"));

        ResponseEntity<String> bad = rest.postForEntity("/api/device/register",
                json("{\"orgApiKey\":\"__NOPE__\",\"name\":\"x\"}"), String.class);
        assertEquals(HttpStatus.BAD_REQUEST, bad.getStatusCode());
    }

    @Test
    void 원격인쇄_왕복_QUEUED_SENT_DONE() {
        Map<String, Object> reg = register("ORG-DEMO-KEY");
        String token = (String) reg.get("deviceToken");
        Long deviceId = ((Number) reg.get("deviceId")).longValue();

        // 관리자: 원격 인쇄 지시(단일 org 폴백으로 테넌트 확정)
        ResponseEntity<Map> enq = rest.exchange("/api/admin/devices/" + deviceId + "/print",
                HttpMethod.POST,
                json("{\"widthMm\":40,\"heightMm\":25,\"dpi\":203,\"elementsJson\":\"[]\",\"variables\":{},\"copies\":1}"),
                Map.class);
        assertEquals(HttpStatus.OK, enq.getStatusCode());

        // 디바이스: 폴링 → 잡 수령(원자 claim → SENT)
        ResponseEntity<Map> next = rest.exchange("/api/device/jobs/next", HttpMethod.GET,
                jsonWithToken("", token), Map.class);
        assertEquals(HttpStatus.OK, next.getStatusCode());
        Long jobId = ((Number) next.getBody().get("id")).longValue();
        assertEquals("SENT", String.valueOf(next.getBody().get("status")));

        // 재폴링 → 없음(204) : 원자 claim 으로 1회만
        ResponseEntity<String> again = rest.exchange("/api/device/jobs/next", HttpMethod.GET,
                jsonWithToken("", token), String.class);
        assertEquals(HttpStatus.NO_CONTENT, again.getStatusCode());

        // 디바이스: ack ok → DONE
        ResponseEntity<Void> ack = rest.exchange("/api/device/jobs/" + jobId + "/ack", HttpMethod.POST,
                jsonWithToken("{\"ok\":true,\"message\":\"printed\"}", token), Void.class);
        assertEquals(HttpStatus.OK, ack.getStatusCode());

        // 관리자: 잡 목록에 DONE
        ResponseEntity<String> jobs = rest.getForEntity("/api/admin/jobs", String.class);
        assertTrue(jobs.getBody().contains("DONE"), "ack 후 DONE 이어야: " + jobs.getBody());
    }

    @Test
    void copies_상한초과_400() {
        Long deviceId = ((Number) register("ORG-DEMO-KEY").get("deviceId")).longValue();
        ResponseEntity<String> r = rest.exchange("/api/admin/devices/" + deviceId + "/print",
                HttpMethod.POST,
                json("{\"widthMm\":40,\"heightMm\":25,\"dpi\":203,\"elementsJson\":\"[]\",\"variables\":{},\"copies\":99999}"),
                String.class);
        assertEquals(HttpStatus.BAD_REQUEST, r.getStatusCode());
    }

    @Test
    void 관리_stats_단일org_폴백() {
        register("ORG-DEMO-KEY");
        ResponseEntity<String> r = rest.getForEntity("/api/admin/stats", String.class);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertTrue(r.getBody().contains("devices"), "stats 에 devices 포함: " + r.getBody());
    }
}
