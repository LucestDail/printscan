package com.printscan.cloud.web;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** JS(대시보드)용 메시지 JSON. 서버 messages 단일 소스. */
@RestController
@RequiredArgsConstructor
public class I18nController {

    private final MessageSource messages;

    private static final List<String> JS_KEYS = List.of(
            "dev.online", "dev.offline",
            "job.status.QUEUED", "job.status.SENT", "job.status.DONE", "job.status.FAILED",
            "empty.devices", "empty.stock", "empty.jobs",
            "toast.selectDevice", "toast.enqueued", "toast.enqueueFail",
            "org.key.previousActive", "org.key.rotatedAt", "org.key.confirmRotate",
            "org.key.rotateFail", "org.key.rotated", "org.key.confirmRevoke",
            "org.key.revoked", "org.key.copied",
            "tpl.addFail"
    );

    @GetMapping("/api/i18n/{lang}.json")
    public Map<String, String> bundle(@PathVariable String lang) {
        Locale loc = Locale.forLanguageTag(lang);
        Map<String, String> out = new LinkedHashMap<>();
        for (String k : JS_KEYS) out.put(k, messages.getMessage(k, null, k, loc));
        return out;
    }
}
