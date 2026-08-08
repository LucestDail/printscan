package com.printscan.edge.web;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** JS 에서 쓰는 메시지를 JSON 으로 제공(단일 소스=messages*.properties). 키 화이트리스트로 내부키 유출 방지. */
@RestController
@RequiredArgsConstructor
public class I18nController {

    private final MessageSource messages;

    /** 정적 JS(scan.js 등)가 참조하는 키만 노출. */
    private static final List<String> JS_KEYS = List.of(
            "scan.in", "scan.out", "scan.low", "scan.notFound", "scan.registerThis",
            "scan.select", "scan.empty.products", "scan.empty.history",
            "toast.lookupErr", "toast.moveDone", "toast.moveFail",
            "toast.codeNameReq", "toast.registerFail", "toast.registered",
            // designer (props panel + toasts, JS 생성)
            "designer.p.type", "designer.p.value", "designer.p.x", "designer.p.y",
            "designer.p.textH", "designer.p.bold", "designer.p.qrSize", "designer.p.gs1", "designer.w", "designer.h",
            "designer.selectEl", "designer.varsNote", "designer.savedTpl",
            "designer.t.jsonErr", "designer.t.batchFail", "designer.t.previewFail", "designer.t.unnamed",
            "designer.t.loadFail", "designer.t.saveFail", "designer.t.saved",
            "designer.t.confirmDel", "designer.t.delFail", "designer.t.deleted"
    );

    @GetMapping("/api/i18n/{lang}.json")
    public Map<String, String> bundle(@PathVariable String lang) {
        Locale loc = Locale.forLanguageTag(lang);
        Map<String, String> out = new LinkedHashMap<>();
        for (String k : JS_KEYS) out.put(k, messages.getMessage(k, null, k, loc));
        return out;
    }
}
