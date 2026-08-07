package com.printscan.edge.label;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 라벨 위의 요소 하나. 좌표·크기는 모두 mm 기준(물리 규격 독립) → 래스터 엔진이 dpi 로 px 변환.
 * value 에 {{key}} 를 쓰면 인쇄 시 변수 치환. 필드는 종류별로 필요한 것만 사용.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LabelElement {
    // ⚠️ @JsonProperty 필수: xMm/yMm 는 getter가 getXMm/getYMm → JavaBeans "대문자 2연속" 규칙상
    //    프로퍼티명이 XMm/YMm 로 잡혀 JSON "xMm"/"yMm" 와 매칭이 깨진다. 전 필드 명시 고정.
    @JsonProperty("type")
    private ElementType type;
    @JsonProperty("xMm")
    private double xMm;          // 좌상단 x
    @JsonProperty("yMm")
    private double yMm;          // 좌상단 y
    @JsonProperty("value")
    private String value = "";   // TEXT 내용 / QR·BARCODE 데이터 (({{var}}) 가능)

    /** TEXT: 글자 높이(mm). QR: 한 변(mm). BARCODE: 높이(mm). BOX: 사용 안 함. */
    @JsonProperty("sizeMm")
    private double sizeMm = 4;
    /** BARCODE/BOX: 폭(mm). 다른 종류는 무시. */
    @JsonProperty("widthMm")
    private double widthMm = 0;
    /** BOX: 높이(mm). 다른 종류는 무시. */
    @JsonProperty("heightMm")
    private double heightMm = 0;
    /** TEXT: 굵게 여부. */
    @JsonProperty("bold")
    private boolean bold = false;
}
