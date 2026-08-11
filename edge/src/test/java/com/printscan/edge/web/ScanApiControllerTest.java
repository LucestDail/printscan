package com.printscan.edge.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 스캔/재고 컨트롤러 통합 — 제품 CRUD·입출고·오류 로케일화 실증(전체 컨텍스트 부팅).
 * 감사 지적("컨트롤러 0% 커버리지") 해소 + 자체 catch 제거(원시 코드 노출 방지) 회귀 고정.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:scantest;DB_CLOSE_DELAY=-1",
        "printscan.cloud.enabled=false",
        "printscan.security.username=admin",
        "printscan.security.password=secret123"
})
class ScanApiControllerTest {

    @Autowired TestRestTemplate rest;

    private TestRestTemplate auth() { return rest.withBasicAuth("admin", "secret123"); }

    private ResponseEntity<String> postJson(String url, String body) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return auth().postForEntity(url, new HttpEntity<>(body, h), String.class);
    }

    @Test
    void 제품_생성_후_중복이면_400_로케일번역_원시코드노출없음() {
        String body = "{\"code\":\"DUP1\",\"name\":\"제품\",\"quantity\":5}";
        assertEquals(HttpStatus.OK, postJson("/api/products", body).getStatusCode());

        ResponseEntity<String> dup = postJson("/api/products", body);
        assertEquals(HttpStatus.BAD_REQUEST, dup.getStatusCode());
        assertTrue(dup.getBody().contains("이미 존재"), "error 필드가 로케일 번역되어야: " + dup.getBody());
        assertTrue(dup.getBody().contains("DUP1"), "제품 코드가 메시지에 치환되어야");
        // error 필드 자체가 원시 코드면 안 됨(code 필드에 코드가 담기는 건 정상).
        assertFalse(dup.getBody().contains("\"error\":\"error.productExists\""), "error 필드에 원시 코드 노출 금지");
    }

    @Test
    void 재고부족_출고_400_로케일번역() {
        postJson("/api/products", "{\"code\":\"LOW1\",\"name\":\"n\",\"quantity\":1}");
        ResponseEntity<String> r = postJson("/api/inventory/move",
                "{\"code\":\"LOW1\",\"type\":\"OUT\",\"qty\":9}");
        assertEquals(HttpStatus.BAD_REQUEST, r.getStatusCode());
        assertTrue(r.getBody().contains("부족"), "error 필드가 로케일 번역되어야: " + r.getBody());
        assertFalse(r.getBody().contains("\"error\":\"error.stockInsufficient\""), "error 필드에 원시 코드 노출 금지");
    }

    @Test
    void 잘못된_type_400() {
        postJson("/api/products", "{\"code\":\"T1\",\"name\":\"n\",\"quantity\":1}");
        ResponseEntity<String> r = postJson("/api/inventory/move",
                "{\"code\":\"T1\",\"type\":\"BOGUS\",\"qty\":1}");
        assertEquals(HttpStatus.BAD_REQUEST, r.getStatusCode());
    }

    @Test
    void 미등록_조회_404() {
        ResponseEntity<String> r = auth().getForEntity("/api/scan/lookup?code=NOPE", String.class);
        assertEquals(HttpStatus.NOT_FOUND, r.getStatusCode());
        assertTrue(r.getBody().contains("false"));
    }

    @Test
    void 입고_출고_왕복_정상() {
        postJson("/api/products", "{\"code\":\"RT1\",\"name\":\"n\",\"quantity\":0}");
        assertEquals(HttpStatus.OK, postJson("/api/inventory/move",
                "{\"code\":\"RT1\",\"type\":\"IN\",\"qty\":10}").getStatusCode());
        ResponseEntity<String> out = postJson("/api/inventory/move",
                "{\"code\":\"RT1\",\"type\":\"OUT\",\"qty\":3}");
        assertEquals(HttpStatus.OK, out.getStatusCode());
        assertTrue(out.getBody().contains("\"resultQty\":7"), "10 입고 후 3 출고 → 7");
    }

    @Test
    void 중복_생성_영문로케일_영문메시지() {
        String body = "{\"code\":\"ENDUP\",\"name\":\"n\",\"quantity\":1}";
        postJson("/api/products", body);
        // ?lang=en → LocaleChangeInterceptor 가 요청 로케일 en 지정
        ResponseEntity<String> dup = postJson("/api/products?lang=en", body);
        assertEquals(HttpStatus.BAD_REQUEST, dup.getStatusCode());
        assertTrue(dup.getBody().toLowerCase().contains("already"), "영문 메시지여야: " + dup.getBody());
    }
}
