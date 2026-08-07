package com.printscan.edge.label;

import com.printscan.edge.config.LineProperties;
import com.printscan.edge.inventory.InventoryService;
import com.printscan.edge.label.raster.LabelRasterizer;
import com.printscan.edge.label.raster.ZplGraphicEncoder;
import com.printscan.edge.print.PrintService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 라벨 템플릿 CRUD + 래스터 미리보기(PNG) + 인쇄(^GFA). 미리보기와 인쇄가 동일 렌더라 화면=인쇄물.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LabelService {

    private final LabelTemplateRepository repository;
    private final LabelRasterizer rasterizer;
    private final PrintService printService;
    private final InventoryService inventory;
    private final LineProperties lineProps;

    // ── CRUD ──────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<LabelTemplate> findAll() { return repository.findAll(); }

    @Transactional(readOnly = true)
    public LabelTemplate get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("템플릿을 찾을 수 없습니다: " + id));
    }

    @Transactional
    public LabelTemplate save(LabelTemplate t) {
        t.setId(null);
        return repository.save(t);
    }

    @Transactional
    public LabelTemplate update(Long id, LabelTemplate patch) {
        LabelTemplate t = get(id);
        if (patch.getName() != null) t.setName(patch.getName());
        t.setDescription(patch.getDescription());
        t.setWidthMm(patch.getWidthMm());
        t.setHeightMm(patch.getHeightMm());
        t.setDpi(patch.getDpi());
        if (patch.getElementsJson() != null) t.setElementsJson(patch.getElementsJson());
        return t; // dirty checking
    }

    @Transactional
    public void delete(Long id) { repository.deleteById(id); }

    // ── 렌더 ──────────────────────────────────────────────
    /** 요청 → 흑백 이미지. id 있으면 저장본 로드, 없으면 인라인 필드로 임시 구성. */
    @Transactional(readOnly = true)
    public BufferedImage render(RenderRequest req) {
        LabelTemplate t = (req.id() != null) ? get(req.id()) : inline(req);
        return rasterizer.render(t, req.variables());
    }

    /** 미리보기 PNG 바이트. */
    public byte[] previewPng(RenderRequest req) {
        try {
            BufferedImage img = render(req);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("미리보기 렌더 실패: " + e.getMessage(), e);
        }
    }

    /** 실제 인쇄: 렌더 → ^GFA → 활성 transport 전송 → 제품코드면 자동 출고(소비 추적). */
    public void print(RenderRequest req) throws Exception {
        int copies = (req.copies() != null && req.copies() > 0) ? req.copies() : 1;
        renderAndSend(req, copies);
        consume(req.variables(), copies, req.operator());
    }

    /** 일련번호 배치 인쇄: seqVar 를 증가시키며 count 장 연속 출력. 소비는 고정 제품코드×count. */
    public void printBatch(RenderRequest base, SerialSpec spec) throws Exception {
        int count = Math.max(1, spec.count());
        for (int i = 0; i < count; i++) {
            Map<String, String> vars = new java.util.LinkedHashMap<>(
                    base.variables() == null ? Map.of() : base.variables());
            vars.put(spec.var(), spec.format(i));
            RenderRequest r = new RenderRequest(base.id(), base.name(), base.widthMm(), base.heightMm(),
                    base.dpi(), base.elementsJson(), vars, 1, base.operator());
            renderAndSend(r, 1);
        }
        log.info("[label] 배치 인쇄 {}장: {}~{}", count, spec.format(0), spec.format(count - 1));
        // 배치 소비: 고정 변수(제품코드 등)를 count 만큼. seq 값은 제품이 아니므로 no-op.
        consume(base.variables(), count, base.operator());
    }

    private void renderAndSend(RenderRequest req, int copies) throws Exception {
        BufferedImage img = render(req);
        String zpl = ZplGraphicEncoder.wrapLabel(img, copies);
        printService.print(zpl);
        log.info("[label] 인쇄: {}x{}px copies={}", img.getWidth(), img.getHeight(), copies);
    }

    /** 인쇄=자동 출고: 변수값 중 등록 제품코드를 qty 만큼 OUT(라인/작업자 귀속). */
    private void consume(Map<String, String> variables, int qty, String operator) {
        if (variables == null) return;
        Set<String> codes = new LinkedHashSet<>(variables.values());
        for (String code : codes) {
            if (code == null || code.isBlank()) continue;
            try {
                inventory.consumeForPrint(code, qty, operator, lineProps.getName());
            } catch (Exception e) {
                log.warn("[label] 자동출고 스킵({}): {}", code, e.getMessage());
            }
        }
    }

    private LabelTemplate inline(RenderRequest req) {
        LabelTemplate t = new LabelTemplate();
        t.setName(req.name() != null ? req.name() : "inline");
        if (req.widthMm() != null) t.setWidthMm(req.widthMm());
        if (req.heightMm() != null) t.setHeightMm(req.heightMm());
        t.setDpi(req.dpi());
        t.setElementsJson(req.elementsJson() != null ? req.elementsJson() : "[]");
        return t;
    }
}
