package com.baeksang.printscan.service.label;

import com.baeksang.printscan.entity.LabelTemplate;
import com.baeksang.printscan.repository.LabelTemplateRepository;
import com.baeksang.printscan.service.PrinterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 라벨 템플릿 CRUD + 변수 바인딩({{key}} 치환) + 인쇄(PrinterService transport 경유).
 */
@Service
@RequiredArgsConstructor
public class LabelTemplateService {

    private final LabelTemplateRepository repository;
    private final PrinterService printerService;

    @Transactional(readOnly = true)
    public List<LabelTemplate> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public LabelTemplate get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("라벨 템플릿을 찾을 수 없습니다: " + id));
    }

    @Transactional
    public LabelTemplate save(LabelTemplate template) {
        return repository.save(template);
    }

    @Transactional
    public LabelTemplate update(Long id, LabelTemplate patch) {
        LabelTemplate t = get(id);
        if (patch.getName() != null) t.setName(patch.getName());
        t.setDescription(patch.getDescription());
        if (patch.getWidthMm() != null) t.setWidthMm(patch.getWidthMm());
        if (patch.getHeightMm() != null) t.setHeightMm(patch.getHeightMm());
        if (patch.getDpi() != null) t.setDpi(patch.getDpi());
        if (patch.getZplBody() != null) t.setZplBody(patch.getZplBody());
        if (patch.getElementsJson() != null) t.setElementsJson(patch.getElementsJson());
        return t; // dirty checking
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    /** 템플릿 zplBody 의 {{key}} 를 variables 로 치환한 최종 ZPL. */
    @Transactional(readOnly = true)
    public String render(Long id, Map<String, String> variables) {
        return bindVariables(get(id).getZplBody(), variables);
    }

    /** {{key}} → value 치환. null 값은 빈 문자열. */
    public static String bindVariables(String zpl, Map<String, String> variables) {
        if (zpl == null) return "";
        if (variables == null || variables.isEmpty()) return zpl;
        String out = zpl;
        for (Map.Entry<String, String> e : variables.entrySet()) {
            out = out.replace("{{" + e.getKey() + "}}", e.getValue() == null ? "" : e.getValue());
        }
        return out;
    }

    /** 템플릿 렌더 후 지정 매수만큼 인쇄. */
    public void print(Long id, Map<String, String> variables, int copies) throws Exception {
        String zpl = render(id, variables);
        if (copies > 1) {
            zpl = withQuantity(zpl, copies);
        }
        printerService.print(zpl);
    }

    /** ^XZ 직전에 ^PQ(매수) 삽입. */
    static String withQuantity(String zpl, int copies) {
        if (zpl == null || !zpl.contains("^XZ")) return zpl;
        int idx = zpl.lastIndexOf("^XZ");
        return zpl.substring(0, idx) + "^PQ" + copies + "\n" + zpl.substring(idx);
    }
}
