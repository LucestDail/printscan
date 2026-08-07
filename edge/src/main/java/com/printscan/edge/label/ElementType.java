package com.printscan.edge.label;

/** 라벨 요소 종류. 래스터 엔진이 종류별로 Java2D 렌더한다. */
public enum ElementType {
    TEXT,     // 텍스트(한글 포함) — CJK 폰트로 렌더
    QR,       // QR 코드 — ZXing
    BARCODE,  // Code128 바코드 — ZXing
    BOX       // 채움 사각형/선
}
