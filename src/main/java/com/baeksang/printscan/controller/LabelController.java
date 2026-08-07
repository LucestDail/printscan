package com.baeksang.printscan.controller;

import com.baeksang.printscan.entity.LabelTemplate;
import com.baeksang.printscan.service.label.LabelTemplateService;
import com.baeksang.printscan.service.label.LabelaryClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 라벨 디자이너 백엔드 — 템플릿 CRUD + Labelary 미리보기 + 실제 인쇄(변수 바인딩).
 * 프론트 캔버스 디자이너가 생성한 zplBody 를 저장/미리보기/출력한다.
 */
@RestController
@RequestMapping("/api/labels")
@RequiredArgsConstructor
public class LabelController {

    private final LabelTemplateService service;
    private final LabelaryClient labelary;

    // ── 템플릿 CRUD ──────────────────────────────
    @GetMapping("/templates")
    public List<LabelTemplate> list() {
        return service.findAll();
    }

    @GetMapping("/templates/{id}")
    public LabelTemplate get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping("/templates")
    public LabelTemplate create(@RequestBody LabelTemplate template) {
        template.setId(null);
        return service.save(template);
    }

    @PutMapping("/templates/{id}")
    public LabelTemplate update(@PathVariable Long id, @RequestBody LabelTemplate patch) {
        return service.update(id, patch);
    }

    @DeleteMapping("/templates/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ── 미리보기(Labelary PNG) ──────────────────────────────
    /** body: { zpl?, templateId?, variables?, dpi?, widthMm?, heightMm? } — zpl 우선, 없으면 templateId 렌더. */
    @PostMapping(value = "/preview", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> preview(@RequestBody PreviewRequest req) {
        try {
            String zpl = req.zpl();
            int dpi = req.dpi() != null ? req.dpi() : 203;
            int w = req.widthMm() != null ? req.widthMm() : 40;
            int h = req.heightMm() != null ? req.heightMm() : 30;
            if ((zpl == null || zpl.isBlank()) && req.templateId() != null) {
                LabelTemplate t = service.get(req.templateId());
                zpl = LabelTemplateService.bindVariables(t.getZplBody(), req.variables());
                if (t.getDpi() != null) dpi = t.getDpi();
                if (t.getWidthMm() != null) w = t.getWidthMm();
                if (t.getHeightMm() != null) h = t.getHeightMm();
            }
            if (zpl == null || zpl.isBlank()) {
                return ResponseEntity.badRequest().build();
            }
            byte[] png = labelary.previewPng(zpl, dpi, w, h);
            return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(png);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // ── 인쇄 ──────────────────────────────
    @PostMapping("/print")
    public ResponseEntity<String> print(@RequestBody PrintRequest req) {
        try {
            int copies = req.copies() != null && req.copies() > 0 ? req.copies() : 1;
            service.print(req.templateId(), req.variables(), copies);
            return ResponseEntity.ok("출력이 완료되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("프린터 오류: " + e.getMessage());
        }
    }

    public record PreviewRequest(String zpl, Long templateId, Map<String, String> variables,
                                 Integer dpi, Integer widthMm, Integer heightMm) {}

    public record PrintRequest(Long templateId, Map<String, String> variables, Integer copies) {}
}
