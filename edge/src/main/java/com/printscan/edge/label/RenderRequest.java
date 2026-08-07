package com.printscan.edge.label;

import java.util.Map;

/**
 * 미리보기/인쇄 요청. id 가 있으면 저장된 템플릿을 로드, 없으면 인라인 필드로 임시 템플릿 구성(디자이너 라이브 프리뷰).
 */
public record RenderRequest(
        Long id,
        String name,
        Double widthMm,
        Double heightMm,
        Integer dpi,
        String elementsJson,
        Map<String, String> variables,
        Integer copies
) {}
