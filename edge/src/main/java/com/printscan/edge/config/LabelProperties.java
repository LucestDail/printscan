package com.printscan.edge.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 라벨 래스터 엔진 설정. CJK 폰트로 한글을 비트맵 렌더 → ^GFA 로 전송하기 위한 폰트 지정.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "printscan.label")
public class LabelProperties {
    /** fontconfig 로 해석되는 폰트 패밀리명(리눅스: Noto Sans CJK KR 등). */
    private String fontFamily = "Noto Sans CJK KR";
    /** 지정 시 해당 TTF/TTC 파일을 직접 로드(패밀리명보다 우선). */
    private String fontPath = "";
    /** 템플릿에 dpi 미지정 시 기본값. */
    private int defaultDpi = 203;
}
