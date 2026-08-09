package com.printscan.edge.web;

import com.printscan.edge.label.LabelService;
import com.printscan.edge.label.LabelTemplate;
import com.printscan.edge.label.RenderRequest;
import com.printscan.edge.label.SerialSpec;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 라벨 REST — 템플릿 CRUD + 래스터 미리보기(PNG) + 인쇄(^GFA). */
@RestController
@RequestMapping("/api/labels")
@RequiredArgsConstructor
public class LabelApiController {

    private final LabelService service;
    private final MessageSource messages;

    private String msg(String code, Object... args) {
        return messages.getMessage(code, args, code, LocaleContextHolder.getLocale());
    }

    @GetMapping("/templates")
    public List<LabelTemplate> list() { return service.findAll(); }

    @GetMapping("/templates/{id}")
    public LabelTemplate get(@PathVariable Long id) { return service.get(id); }

    @PostMapping("/templates")
    public LabelTemplate create(@RequestBody LabelTemplate t) { return service.save(t); }

    @PutMapping("/templates/{id}")
    public LabelTemplate update(@PathVariable Long id, @RequestBody LabelTemplate patch) {
        return service.update(id, patch);
    }

    @DeleteMapping("/templates/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** 미리보기 PNG (서버 래스터 = 인쇄물과 동일). */
    @PostMapping(value = "/preview", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> preview(@RequestBody RenderRequest req) {
        try {
            return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(service.previewPng(req));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private static final int MAX_COPIES = 1000;

    /** 실제 인쇄. */
    @PostMapping("/print")
    public ResponseEntity<String> print(@RequestBody RenderRequest req) {
        if (req.copies() != null && (req.copies() < 1 || req.copies() > MAX_COPIES))
            throw new ApiException("error.copiesRange", MAX_COPIES);
        try {
            service.print(req);
            return ResponseEntity.ok(msg("print.done"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(msg("print.printerError", e.getMessage()));
        }
    }

    /** 일련번호 자동증가 배치 인쇄. */
    @PostMapping("/print-batch")
    public ResponseEntity<String> printBatch(@RequestBody BatchRequest b) {
        if (b.count() < 1 || b.count() > MAX_COPIES)
            throw new ApiException("error.copiesRange", MAX_COPIES);
        if (b.pad() < 0 || b.pad() > 12)
            throw new ApiException("error.padRange");
        try {
            RenderRequest base = new RenderRequest(b.id(), b.name(), b.widthMm(), b.heightMm(), b.dpi(),
                    b.elementsJson(), b.variables(), 1, b.operator());
            SerialSpec spec = new SerialSpec(b.seqVar(), b.prefix(), b.start(), b.count(), b.pad());
            service.printBatch(base, spec);
            return ResponseEntity.ok(msg("print.batchDone", b.count(), spec.format(0), spec.format(b.count() - 1)));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(msg("print.printerError", e.getMessage()));
        }
    }

    /** mm 눈금자 미리보기(PNG) — 자 없이 인쇄 가능폭 확인. */
    @GetMapping(value = "/ruler", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> ruler(@RequestParam(defaultValue = "60") int widthMm,
                                        @RequestParam(defaultValue = "25") int heightMm,
                                        @RequestParam(defaultValue = "203") int dpi) {
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(service.rulerPng(widthMm, heightMm, dpi));
    }

    /** mm 눈금자 실제 인쇄. */
    @PostMapping("/ruler/print")
    public ResponseEntity<String> printRuler(@RequestParam(defaultValue = "60") int widthMm,
                                             @RequestParam(defaultValue = "25") int heightMm,
                                             @RequestParam(defaultValue = "203") int dpi) {
        try {
            service.printRuler(widthMm, heightMm, dpi);
            return ResponseEntity.ok(msg("print.rulerDone"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(msg("print.printerError", e.getMessage()));
        }
    }

    /** 미디어 자동 캘리브레이션(~JC) — 라벨 크기/갭 재측정으로 잘림/치우침 교정. */
    @PostMapping("/calibrate")
    public ResponseEntity<String> calibrate() {
        try {
            service.calibrate();
            return ResponseEntity.ok(msg("print.calibrateDone"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(msg("print.printerError", e.getMessage()));
        }
    }

    public record BatchRequest(Long id, String name, Double widthMm, Double heightMm, Integer dpi,
                               String elementsJson, Map<String, String> variables, String operator,
                               String seqVar, String prefix, int start, int count, int pad) {}
}
