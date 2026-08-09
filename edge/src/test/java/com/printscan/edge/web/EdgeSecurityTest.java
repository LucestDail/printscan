package com.printscan.edge.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * edge 보안 통합 테스트 — 전체 컨텍스트 부팅(모든 빈 배선 검증) + HTTP Basic 401/200 실증.
 * 감사 지적("auth 401/200 증명 테스트 전무") 해소.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:sectest;DB_CLOSE_DELAY=-1",
        "printscan.cloud.enabled=false",
        "printscan.security.username=admin",
        "printscan.security.password=secret123"
})
class EdgeSecurityTest {

    @Autowired TestRestTemplate rest;

    @Test
    void 무인증_API는_401() {
        ResponseEntity<String> r = rest.getForEntity("/api/products", String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, r.getStatusCode());
    }

    @Test
    void Basic인증_API는_200() {
        ResponseEntity<String> r = rest.withBasicAuth("admin", "secret123")
                .getForEntity("/api/products", String.class);
        assertEquals(HttpStatus.OK, r.getStatusCode());
    }

    @Test
    void 잘못된_비번_401() {
        ResponseEntity<String> r = rest.withBasicAuth("admin", "wrong")
                .getForEntity("/api/products", String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, r.getStatusCode());
    }

    @Test
    void actuator_health는_무인증_개방() {
        // permitAll 검증: 401 이 아니어야 함. 프린터 부재 테스트환경에선 printer 헬스 DOWN → 503 정상
        // (헬스 인디케이터가 실제로 동작한다는 증거이기도 함).
        ResponseEntity<String> r = rest.getForEntity("/actuator/health", String.class);
        assertNotEquals(HttpStatus.UNAUTHORIZED, r.getStatusCode()); // permitAll(401 아님). 200(UP) 또는 503(printer 부재 DOWN)
    }
}
