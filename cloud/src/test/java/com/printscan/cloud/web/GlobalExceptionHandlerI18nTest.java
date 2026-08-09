package com.printscan.cloud.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.ResponseEntity;

import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** cloud ApiException 이 요청 로케일로 번역되는지 검증. */
class GlobalExceptionHandlerI18nTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(messageSource());

    private static ResourceBundleMessageSource messageSource() {
        ResourceBundleMessageSource ms = new ResourceBundleMessageSource();
        ms.setBasename("messages");
        ms.setDefaultEncoding("UTF-8");
        ms.setFallbackToSystemLocale(false);
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
    void badOrgKey_로케일별_번역() {
        ApiException e = new ApiException("error.badOrgKey");
        assertEquals("잘못된 조직 키입니다", errorFor(Locale.KOREAN, e));
        assertEquals("Invalid organization key", errorFor(Locale.ENGLISH, e));
        assertEquals("Khóa tổ chức không hợp lệ", errorFor(new Locale("vi"), e));
        assertEquals("Kunci organisasi tidak valid", errorFor(new Locale("id"), e));
    }

    @Test
    void copiesMax_인자_치환() {
        ApiException e = new ApiException("error.copiesMax", 1000);
        assertNotEquals("error.copiesMax", errorFor(Locale.ENGLISH, e));
    }
}
