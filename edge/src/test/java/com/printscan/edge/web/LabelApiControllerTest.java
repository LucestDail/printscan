package com.printscan.edge.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 라벨 컨트롤러 통합 — 래스터 미리보기(PNG)·입력 상한·템플릿 CRUD(프린터 불요 경로).
 * 실제 인쇄(print/calibrate)는 프린터 필요라 제외; 렌더·검증·CRUD 만.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:labeltest;DB_CLOSE_DELAY=-1",
        "printscan.cloud.enabled=false",
        "printscan.security.username=admin",
        "printscan.security.password=secret123"
})
class LabelApiControllerTest {

    @Autowired TestRestTemplate rest;

    private TestRestTemplate auth() { return rest.withBasicAuth("admin", "secret123"); }

    private HttpEntity<String> json(String body) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, h);
    }

    @Test
    void 미리보기_PNG_200() {
        ResponseEntity<byte[]> r = auth().postForEntity("/api/labels/preview",
                json("{\"widthMm\":60,\"heightMm\":25,\"dpi\":203,\"elementsJson\":\"[{\\\"type\\\":\\\"TEXT\\\",\\\"xMm\\\":2,\\\"yMm\\\":2,\\\"value\\\":\\\"한글 TEST\\\",\\\"sizeMm\\\":3}]\",\"variables\":{}}"),
                byte[].class);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals(MediaType.IMAGE_PNG, r.getHeaders().getContentType());
        assertTrue(r.getBody() != null && r.getBody().length > 100, "PNG 바이트가 있어야");
    }

    @Test
    void 인쇄_매수상한초과_400_프린터도달전차단() {
        ResponseEntity<String> r = auth().postForEntity("/api/labels/print",
                json("{\"copies\":99999,\"widthMm\":60,\"heightMm\":25,\"dpi\":203,\"elementsJson\":\"[]\"}"),
                String.class);
        assertEquals(HttpStatus.BAD_REQUEST, r.getStatusCode());
    }

    @Test
    void 눈금자_PNG_200() {
        ResponseEntity<byte[]> r = auth().getForEntity("/api/labels/ruler?widthMm=60&heightMm=25&dpi=203", byte[].class);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals(MediaType.IMAGE_PNG, r.getHeaders().getContentType());
    }

    @Test
    void 템플릿_CRUD_왕복() {
        // 생성
        ResponseEntity<String> created = auth().postForEntity("/api/labels/templates",
                json("{\"name\":\"T1\",\"widthMm\":40,\"heightMm\":25,\"dpi\":203,\"elementsJson\":\"[]\"}"),
                String.class);
        assertEquals(HttpStatus.OK, created.getStatusCode());
        Long id = Long.valueOf(created.getBody().replaceAll(".*\"id\":(\\d+).*", "$1"));

        // 조회
        assertEquals(HttpStatus.OK, auth().getForEntity("/api/labels/templates/" + id, String.class).getStatusCode());
        // 목록
        assertTrue(auth().getForEntity("/api/labels/templates", String.class).getBody().contains("T1"));
        // 삭제
        auth().delete("/api/labels/templates/" + id);
        // 삭제 후 조회 → 400(error.templateNotFound)
        assertEquals(HttpStatus.BAD_REQUEST, auth().getForEntity("/api/labels/templates/" + id, String.class).getStatusCode());
    }
}
