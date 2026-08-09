package com.printscan.cloud.i18n;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;

/** 허브 로케일(en/vi/id)이 기준(ko) 키를 전부 보유하는지 검증. */
class I18nCompletenessTest {

    private Properties load(String name) throws Exception {
        Properties p = new Properties();
        try (InputStream in = getClass().getResourceAsStream("/" + name)) {
            assertNotNull(in, name + " 없음");
            p.load(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
        }
        return p;
    }

    @Test
    void 허브_모든_로케일이_ko_키를_전부_보유() throws Exception {
        Properties ko = load("messages.properties");
        for (String loc : new String[]{"messages_en.properties", "messages_vi.properties", "messages_id.properties"}) {
            Properties other = load(loc);
            Set<String> missing = new TreeSet<>();
            for (String k : ko.stringPropertyNames()) {
                if (!other.containsKey(k) || other.getProperty(k).isBlank()) missing.add(k);
            }
            assertTrue(missing.isEmpty(), loc + " 누락 키: " + missing);
        }
    }
}
