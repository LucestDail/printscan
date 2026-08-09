package com.printscan.edge.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.ResponseEntity;

import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** ApiException 이 요청 로케일로 번역되는지(원시 한국어 노출 방지) 검증. */
class GlobalExceptionHandlerI18nTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(messageSource());

    private static ResourceBundleMessageSource messageSource() {
        ResourceBundleMessageSource ms = new ResourceBundleMessageSource();
        ms.setBasename("messages");
        ms.setDefaultEncoding("UTF-8");
        ms.setFallbackToSystemLocale(false); // 로케일 미존재 시 시스템 로케일로 새지 않게
        return ms;
    }

    @AfterEach
    void reset() { LocaleContextHolder.resetLocaleContext(); }

    private String errorFor(Locale locale, ApiException e) {
        LocaleContextHolder.setLocale(locale);
        ResponseEntity<Map<String, String>> res = handler.api(e);
        assertEquals(400, res.getStatusCode().value());
        assertEquals(e.getCode(), res.getBody().get("code"));
        return res.getBody().get("error");
    }

    @Test
    void copiesRange_로케일별_번역() {
        ApiException e = new ApiException("error.copiesRange", 1000);
        assertEquals("매수는 1~1,000 범위여야 합니다.", errorFor(Locale.KOREAN, e));
        assertEquals("Quantity must be between 1 and 1,000.", errorFor(Locale.ENGLISH, e));
        assertEquals("Số lượng phải từ 1 đến 1.000.", errorFor(new Locale("vi"), e));
        assertEquals("Jumlah harus antara 1 dan 1.000.", errorFor(new Locale("id"), e));
    }

    @Test
    void stockInsufficient_인자_치환() {
        ApiException e = new ApiException("error.stockInsufficient", 3, 5);
        assertEquals("재고가 부족합니다(현재=3, 출고=5).", errorFor(Locale.KOREAN, e));
        assertEquals("Insufficient stock (current=3, out=5).", errorFor(Locale.ENGLISH, e));
    }

    @Test
    void 미지원_로케일은_영문_기본으로_안전_폴백() {
        // 프랑스어 번들 없음 → error.copiesRange 키가 없으면 코드가 그대로 노출되면 안 됨.
        // ResourceBundle 폴백(fallbackToSystemLocale=false) 규칙상 기본 번들(ko)로 떨어짐 → 최소 코드 노출은 아님.
        ApiException e = new ApiException("error.copiesRange", 1000);
        String msg = errorFor(Locale.FRENCH, e);
        assertNotEquals("error.copiesRange", msg, "번역 실패로 코드가 그대로 노출되면 안 된다");
    }
}
