package com.baeksang.printscan.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 라벨 템플릿 — 라벨 디자이너로 만든 ZPL 본문과 규격을 저장.
 * zplBody 에 {{placeholder}} 를 넣어두면 인쇄 시 제품 필드 등으로 치환(변수 바인딩).
 */
@Entity
@Table(name = "label_template")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabelTemplate extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    /** 라벨 물리 규격(mm) */
    private Integer widthMm;
    private Integer heightMm;

    /** 프린터 해상도: 203 | 300 */
    private Integer dpi;

    /** {{placeholder}} 를 포함할 수 있는 ZPL 본문 */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String zplBody;

    /** 프론트 캔버스 디자이너의 요소 직렬화(JSON, 재편집용, 선택) */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String elementsJson;
}
