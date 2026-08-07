package com.printscan.edge.web;

import com.printscan.edge.label.LabelService;
import com.printscan.edge.label.LabelTemplate;
import com.printscan.edge.label.RenderRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 라벨 REST — 템플릿 CRUD + 래스터 미리보기(PNG) + 인쇄(^GFA). */
@RestController
@RequestMapping("/api/labels")
@RequiredArgsConstructor
public class LabelApiController {

    private final LabelService service;

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

    /** 실제 인쇄. */
    @PostMapping("/print")
    public ResponseEntity<String> print(@RequestBody RenderRequest req) {
        try {
            service.print(req);
            return ResponseEntity.ok("출력이 완료되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("프린터 오류: " + e.getMessage());
        }
    }
}
